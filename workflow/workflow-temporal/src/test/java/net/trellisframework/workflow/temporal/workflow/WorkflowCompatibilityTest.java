package net.trellisframework.workflow.temporal.workflow;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import static com.jayway.jsonpath.internal.path.PathCompiler.fail;


class WorkflowCompatibilityTest {

    @Test
    void verify() throws Exception {
        String baseRef = System.getProperty("workflow.base", "HEAD^");
        List<String> violations = changedJavaFiles(baseRef)
                .stream()
                .map(file -> validateWorkflowsChanges(file, baseRef))
                .flatMap(Optional::stream)
                .toList();
        if (!violations.isEmpty()) {
            fail("""
                    Workflow compatibility validation failed:
                    %s
                    """.formatted(String.join("\n\n", violations)));
        }
    }

    private Optional<String> validateWorkflowsChanges(Path currentFile, String baseRef) {
        try {
            String currentSource = Files.readString(currentFile);
            CompilationUnit currentUnit = StaticJavaParser.parse(currentSource);
            if (!isWorkflow(currentUnit)) {
                return Optional.empty();
            }
            String oldSource = gitShow(baseRef, currentFile);
            CompilationUnit oldUnit = oldSource == null
                    ? null
                    : StaticJavaParser.parse(oldSource);
            int oldVersionCalls = oldUnit == null
                    ? 0
                    : versionCallCount(oldUnit);
            int currentVersionCalls = versionCallCount(currentUnit);
            boolean newVersionCallWasAdded = currentVersionCalls > oldVersionCalls;
            if (newVersionCallWasAdded) {
                return Optional.empty();
            }
            Optional<String> replaySafeReason = replaySafeReason(currentUnit);
            if (replaySafeReason.isPresent()
                && !replaySafeReason.get().isBlank()) {
                return Optional.empty();
            }
            return Optional.of("""
                    File: %s
                    A @Workflow class was changed, but no new version()
                    or Workflow.getVersion() call was added.

                    If this is genuinely replay-safe, add:
                    @ReplaySafeChange(reason = "explain why workflow history is unchanged")
                    """.formatted(currentFile));

        } catch (Exception exception) {
            return Optional.of("""
                    File: %s
                    Could not validate workflow: %s
                    """.formatted(currentFile, exception.getMessage()));
        }
    }

    private boolean isWorkflow(CompilationUnit unit) {
        return unit
                .findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .anyMatch(type ->
                        type.getAnnotationByName("Workflow").isPresent());
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

    private Optional<String> replaySafeReason(CompilationUnit unit) {
        return unit
                .findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .filter(type ->
                        type.getAnnotationByName("TemporalSafeChange")
                                .isPresent())
                .flatMap(type ->
                        type.getAnnotationByName("TemporalSafeChange")
                                .stream())
                .flatMap(annotation ->
                        annotation.toNormalAnnotationExpr()
                                .map(NormalAnnotationExpr::getPairs)
                                .orElse(new NodeList<>())
                                .stream())
                .filter(pair ->
                        pair.getNameAsString().equals("reason"))
                .map(pair ->
                        pair.getValue().toString()
                                .replace("\"", ""))
                .findFirst();
    }

    private List<Path> changedJavaFiles(String baseRef) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                "git",
                "diff",
                "--name-only",
                "--diff-filter=ACMRTUXB",
                baseRef + "...HEAD",
                "--",
                "*.java"
        ).redirectErrorStream(true).start();
        List<Path> files;
        try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
            files = reader.lines()
                    .map(Path::of)
                    .filter(Files::exists)
                    .toList();
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException(
                    "Unable to determine changed Java files");
        }
        return files;
    }

    private String gitShow(String baseRef, Path file) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                "git",
                "show",
                baseRef + ":" + file).redirectErrorStream(true).start();
        String source = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        return exitCode == 0 ? source : null;
    }
}