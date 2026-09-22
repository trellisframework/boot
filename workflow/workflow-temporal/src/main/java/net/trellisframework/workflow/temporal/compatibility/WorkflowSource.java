package net.trellisframework.workflow.temporal.compatibility;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class WorkflowSource {

    private static final JavaParser PARSER = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    private static final Set<String> COMMAND_SCOPES = Set.of("Workflow", "Async");
    private static final Set<String> COMMAND_NAMES = Set.of(
            "call", "callAsync", "await", "sleep", "sleepMinutes",
            "version", "isVersion", "getVersion", "newActivityStub",
            "executeActivity", "newChildWorkflowStub", "continueAsNew",
            "sideEffect", "mutableSideEffect", "newExternalWorkflowStub",
            "signalExternalWorkflow");
    private static final Set<String> VERSION_NAMES = Set.of("version", "isVersion", "getVersion");
    private static final Set<Class<?>> BRANCHES = Set.of(IfStmt.class, ConditionalExpr.class, SwitchStmt.class,
            SwitchEntry.class,
            ForStmt.class, ForEachStmt.class, WhileStmt.class, DoStmt.class, TryStmt.class, CatchClause.class);
    private static final Set<Class<?>> JUMPS = Set.of(ReturnStmt.class, ThrowStmt.class, BreakStmt.class, ContinueStmt.class, YieldStmt.class);

    private final List<ClassOrInterfaceDeclaration> workflows;

    private WorkflowSource(CompilationUnit unit) {
        workflows = unit.findAll(ClassOrInterfaceDeclaration.class, type -> type.getAnnotationByName("Workflow").isPresent());
    }

    static WorkflowSource parse(String source) {
        return PARSER.parse(source).getResult().map(WorkflowSource::new)
                .orElseThrow(() -> new IllegalArgumentException("Unable to parse Java source"));
    }

    boolean isWorkflow() {
        return !workflows.isEmpty();
    }

    String skeleton() {
        return workflows.stream().map(type -> new Skeleton(type).build()).collect(Collectors.joining("\n"));
    }

    Map<String, Integer> versions() {
        Map<String, Integer> versions = new HashMap<>();
        workflows.stream()
                .flatMap(type -> type.findAll(MethodCallExpr.class, call -> VERSION_NAMES.contains(call.getNameAsString())).stream())
                .filter(call -> call.getArguments().size() >= 2)
                .forEach(call -> versions.merge(literal(call.getArgument(0)), number(call.getArgument(call.getArguments().size() - 1)), Math::max));
        return versions;
    }

    boolean advancesVersionsOf(WorkflowSource base) {
        Map<String, Integer> before = base.versions();
        Map<String, Integer> after = versions();
        boolean preserved = before.entrySet().stream().allMatch(entry -> after.getOrDefault(entry.getKey(), -1) >= entry.getValue());
        return preserved && !after.equals(before);
    }

    Optional<String> safeChangeReason() {
        return workflows.stream()
                .flatMap(type -> type.getAnnotationByName("TemporalSafeChange").stream())
                .filter(AnnotationExpr::isNormalAnnotationExpr)
                .flatMap(annotation -> annotation.asNormalAnnotationExpr().getPairs().stream())
                .filter(pair -> pair.getNameAsString().equals("reason"))
                .map(pair -> literal(pair.getValue()))
                .filter(reason -> !reason.isBlank())
                .findFirst();
    }

    private static int number(Expression expression) {
        return expression.isIntegerLiteralExpr() ? expression.asIntegerLiteralExpr().asNumber().intValue() : 0;
    }

    private static String literal(Expression expression) {
        return expression.isStringLiteralExpr() ? expression.asStringLiteralExpr().asString() : expression.toString();
    }

    private static boolean isCommand(MethodCallExpr call) {
        return COMMAND_NAMES.contains(call.getNameAsString())
                || call.getScope().map(scope -> COMMAND_SCOPES.contains(scope.toString())).orElse(false);
    }

    private static final class Skeleton {

        private final StringBuilder out = new StringBuilder();
        private final ClassOrInterfaceDeclaration type;
        private final Map<String, String> locals = new HashMap<>();

        Skeleton(ClassOrInterfaceDeclaration type) {
            this.type = type;
            type.findAll(Parameter.class).forEach(parameter -> local(parameter.getNameAsString()));
            type.findAll(TypePatternExpr.class).forEach(pattern -> local(pattern.getNameAsString()));
            type.findAll(VariableDeclarationExpr.class).forEach(declaration ->
                    declaration.getVariables().forEach(variable -> local(variable.getNameAsString())));
        }

        String build() {
            visit(type);
            relevantValues();
            return out.toString();
        }

        private void local(String name) {
            locals.putIfAbsent(name, "$" + (locals.size() + 1));
        }

        private void visit(Node node) {
            if (node instanceof MethodCallExpr call && isCommand(call))
                out.append(text(call)).append(';');
            else if (JUMPS.contains(node.getClass()))
                block(node, "", node.getChildNodes());
            else if (BRANCHES.contains(node.getClass()) && steers(node))
                block(node, text(node.getChildNodes().stream().filter(child -> child instanceof Expression || child instanceof Parameter).toList()),
                        node.getChildNodes().stream().filter(child -> !(child instanceof Expression)).toList());
            else
                node.getChildNodes().forEach(this::visit);
        }

        private void block(Node node, String header, List<Node> bodies) {
            out.append(node.getClass().getSimpleName()).append('(').append(header).append(')');
            for (Node body : bodies) {
                out.append('{');
                visit(body);
                out.append('}');
            }
        }

        private boolean steers(Node node) {
            return node.findFirst(MethodCallExpr.class, WorkflowSource::isCommand).isPresent()
                    || node.findFirst(Statement.class, statement -> JUMPS.contains(statement.getClass())).isPresent();
        }

        private void relevantValues() {
            Set<String> names = type.findAll(MethodCallExpr.class, WorkflowSource::isCommand)
                    .stream()
                    .flatMap(call -> call.findAll(NameExpr.class).stream())
                    .map(NameExpr::getNameAsString)
                    .collect(Collectors.toSet());

            type.findAll(VariableDeclarator.class)
                    .stream()
                    .filter(variable -> names.contains(variable.getNameAsString()))
                    .forEach(variable -> out.append("value(")
                            .append(text(variable))
                            .append(");"));

            type.findAll(AssignExpr.class)
                    .stream()
                    .filter(assignment -> assignment.getTarget()
                            .findAll(NameExpr.class)
                            .stream()
                            .anyMatch(name -> names.contains(name.getNameAsString())))
                    .forEach(assignment -> out.append("value(")
                            .append(text(assignment))
                            .append(");"));
        }

        private String text(List<Node> nodes) {
            return nodes.stream().map(this::text).collect(Collectors.joining(","));
        }

        private String text(Node node) {
            Node copy = node.clone();
            copy.findAll(SimpleName.class, name -> locals.containsKey(name.getIdentifier())).forEach(name -> name.setIdentifier(locals.get(name.getIdentifier())));
            return copy.toString().replaceAll("\\s+", " ").trim();
        }
    }
}
