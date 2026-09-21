package net.trellisframework.workflow.temporal.workflow;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

class WorkflowCompatibilityTest {

    private static final JavaParser JAVA_PARSER = new JavaParser(
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));

    private static final String TEMPORAL_SAFE_CHANGE_ANNOTATION = "TemporalSafeChange";

    private static final Set<String> TEMPORAL_COMMANDS = Set.of(
            "call",
            "version",
            "getVersion",
            "sleep",
            "await",
            "newActivityStub",
            "executeActivity",
            "newChildWorkflowStub",
            "continueAsNew",
            "sideEffect",
            "mutableSideEffect",
            "newExternalWorkflowStub",
            "signalExternalWorkflow"
    );

    private static final Pattern REASON_PATTERN = Pattern.compile(
            "reason\\s*=\\s*\"([^\"]+)\""
    );

    @Test
    void verify() throws Exception {
        String baseRef = System.getProperty("workflow.base", "HEAD");
        List<String> violations = changedJavaFiles(baseRef)
                .stream()
                .map(file -> validateWorkflowChange(file, baseRef))
                .flatMap(Optional::stream)
                .toList();
        if (!violations.isEmpty()) {
            Assertions.fail("""
                    Workflow compatibility validation failed:
                    %s
                    """.formatted(String.join("\n\n", violations)));
        }
    }

    private Optional<String> validateWorkflowChange(Path currentFile, String baseRef) {
        try {
            CompilationUnit currentUnit = parse(currentFile);
            if (!isWorkflow(currentUnit)) {
                return Optional.empty();
            }
            String oldSource = gitShow(baseRef, currentFile);
            CompilationUnit oldUnit = oldSource == null
                    ? null
                    : parse(oldSource);
            int oldVersionCalls = oldUnit == null
                    ? 0
                    : versionCallCount(oldUnit);
            int currentVersionCalls = versionCallCount(currentUnit);
            boolean newVersionCallWasAdded =
                    currentVersionCalls > oldVersionCalls;
            boolean workflowBehaviorChanged = oldUnit == null || !behaviorFingerprint(oldUnit).equals(behaviorFingerprint(currentUnit));
            if (workflowBehaviorChanged && newVersionCallWasAdded) {
                return Optional.empty();
            }
            if (!workflowBehaviorChanged
                && replaySafeReason(currentUnit).isPresent()) {
                return Optional.empty();
            }
            if (workflowBehaviorChanged) {
                return Optional.of("""
                        File: %s
                        The workflow's Temporal commands or workflow decisions changed,
                        but no new version() or Workflow.getVersion() call was added.

                        Add a new stable change ID, for example:
                        version("prospect-flow-v2", 1)
                        """.formatted(currentFile.toAbsolutePath()));
            }
            return Optional.of("""
                    File: %s
                    The @Workflow class changed, but the change is not marked
                    as safe-change.

                    For a replay-safe change, add:
                    @TemporalSafeChange(reason = "explain why workflow history is unchanged")
                    """.formatted(currentFile.toAbsolutePath()));
        } catch (Exception exception) {
            return Optional.of("""
                    File: %s
                    Could not validate workflow: %s
                    """.formatted(currentFile.toAbsolutePath(),
                    exception.getMessage()));
        }
    }

    private boolean isWorkflow(CompilationUnit unit) {
        return unit
                .findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .anyMatch(type ->
                        type.getAnnotationByName("Workflow").isPresent());
    }

    private CompilationUnit parse(Path file) throws IOException {
        return JAVA_PARSER
                .parse(file)
                .getResult()
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to parse Java source: " + file));
    }

    private CompilationUnit parse(String source) {
        return JAVA_PARSER
                .parse(source)
                .getResult()
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to parse Java source from Git baseline"));
    }

    private int versionCallCount(CompilationUnit unit) {
        return (int) unit
                .findAll(MethodCallExpr.class)
                .stream()
                .filter(this::isVersionCall)
                .count();
    }

    private boolean isVersionCall(MethodCallExpr call) {
        String methodName = call.getNameAsString();
        return methodName.equals("version") || methodName.equals("getVersion");
    }

    private String behaviorFingerprint(CompilationUnit unit) {
        Set<String> localNames = Stream.concat(
                        unit.findAll(VariableDeclarator.class)
                                .stream()
                                .map(VariableDeclarator::getNameAsString),
                        unit.findAll(Parameter.class)
                                .stream()
                                .map(Parameter::getNameAsString)).collect(java.util.stream.Collectors.toSet());
        String commands = unit
                .findAll(MethodCallExpr.class)
                .stream()
                .filter(call -> TEMPORAL_COMMANDS.contains(call.getNameAsString()))
                .map(call -> normalize(call, localNames))
                .toList()
                .toString();

        String decisions = decisionNodes(unit)
                .map(node -> normalize(node, localNames))
                .toList()
                .toString();

        return commands + "|" + decisions;
    }

    private Stream<Node> decisionNodes(CompilationUnit unit) {
        Stream<Stream<Node>> streams = Stream.of(
                unit.findAll(IfStmt.class).stream().map(node -> (Node) node),
                unit.findAll(SwitchStmt.class).stream().map(node -> (Node) node),
                unit.findAll(ForStmt.class).stream().map(node -> (Node) node),
                unit.findAll(ForEachStmt.class).stream().map(node -> (Node) node),
                unit.findAll(WhileStmt.class).stream().map(node -> (Node) node),
                unit.findAll(DoStmt.class).stream().map(node -> (Node) node)
        );

        return streams.flatMap(stream -> stream);
    }

    private String normalize(Node node, Set<String> localNames) {
        Node normalized = node.clone();
        normalized.findAll(NameExpr.class)
                .stream()
                .filter(name -> localNames.contains(name.getNameAsString()))
                .forEach(name -> name.setName("$local"));

        return normalized.toString().replaceAll("\\s+", " ").trim();
    }

    private Optional<String> replaySafeReason(CompilationUnit unit) {
        return unit
                .findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .flatMap(type -> type.getAnnotations().stream())
                .filter(this::isReplaySafeAnnotation)
                .map(AnnotationExpr::toString)
                .map(REASON_PATTERN::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1).trim())
                .filter(reason -> !reason.isBlank())
                .findFirst();
    }

    private boolean isReplaySafeAnnotation(AnnotationExpr annotation) {
        return TEMPORAL_SAFE_CHANGE_ANNOTATION.equals(annotation.getNameAsString());
    }

    private List<Path> changedJavaFiles(String baseRef) throws IOException, InterruptedException {
        Path repositoryRoot = repositoryRoot();
        String output = runGit(
                repositoryRoot,
                "diff",
                "--name-only",
                "--diff-filter=ACMRTUXB",
                baseRef,
                "--",
                "*.java"
        );
        return Arrays.stream(output.split("\\R"))
                .filter(path -> !path.isBlank())
                .map(repositoryRoot::resolve)
                .map(Path::normalize)
                .filter(Files::isRegularFile)
                .toList();
    }

    private String gitShow(String baseRef, Path file) throws IOException, InterruptedException {
        Path repositoryRoot = repositoryRoot();
        String relativePath = repositoryRoot
                .relativize(file.toAbsolutePath().normalize())
                .toString();
        Process process = new ProcessBuilder(
                "git",
                "show",
                baseRef + ":" + relativePath)
                .directory(repositoryRoot.toFile())
                .redirectErrorStream(true)
                .start();
        String source = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return process.waitFor() == 0 ? source : null;
    }

    private Path repositoryRoot() throws IOException, InterruptedException {
        return Paths.get(runGit(null, "rev-parse", "--show-toplevel"))
                .toAbsolutePath()
                .normalize();
    }

    private String runGit(Path directory, String... arguments) throws IOException, InterruptedException {
        List<String> command = Stream.concat(Stream.of("git"), Arrays.stream(arguments)).toList();
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        if (directory != null) {
            builder.directory(directory.toFile());
        }
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.waitFor() != 0) {
            throw new IllegalStateException(
                    "Git command failed: " + String.join(" ", command)
                    + "\n" + output);
        }
        return output;
    }
}