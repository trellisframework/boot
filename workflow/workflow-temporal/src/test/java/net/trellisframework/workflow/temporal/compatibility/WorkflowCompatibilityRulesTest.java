package net.trellisframework.workflow.temporal.compatibility;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowCompatibilityRulesTest {

    private static final String BODY = """
            public void run(String orderId) {
                var order = call(LoadOrder.class, orderId);
                if (order.isEmpty()) return;
                call(ChargeCard.class, order);
            }
            """;

    @Test
    void ignoresFilesWithoutWorkflowClass() {
        String plain = "class Helper { void run(String id) { call(LoadOrder.class, id); } }";
        assertTrue(verify(plain, plain.replace("id)", "id) { }")).isEmpty());
    }

    @Test
    void acceptsNewWorkflow() {
        assertTrue(WorkflowCompatibility.verify(Optional.empty(), workflow(BODY)).isEmpty());
    }

    @Test
    void renamedLocalIsNotABehaviourChange() {
        String renamed = BODY.replace("order", "loaded");
        assertNeedsSafeChange(verify(workflow(BODY), workflow(renamed)));
        assertTrue(verify(workflow(BODY), workflow("locals renamed", renamed)).isEmpty());
    }

    @Test
    void plainStatementsAroundCommandsAreNotABehaviourChange() {
        String logged = BODY.replace("if (order.isEmpty()) return;", "log.info(\"loaded {}\", order);\nif (order.isEmpty()) return;");
        assertNeedsSafeChange(verify(workflow(BODY), workflow(logged)));
    }

    @Test
    void addedCommandRequiresNewChangeId() {
        String added = BODY.replace("call(ChargeCard.class, order);", "call(ChargeCard.class, order);\ncall(SendReceipt.class, order);");
        assertNeedsChangeId(verify(workflow(BODY), workflow(added)));
        assertNeedsChangeId(verify(workflow(BODY), workflow("just a receipt", added)));
    }

    @Test
    void addedCommandGuardedByNewChangeIdIsAccepted() {
        String versioned = BODY.replace("call(ChargeCard.class, order);",
                "call(ChargeCard.class, order);\nif (isVersion(\"receipt-v1\", 1)) call(SendReceipt.class, order);");
        assertTrue(verify(workflow(BODY), workflow(versioned)).isEmpty());
    }

    @Test
    void reusingAnExistingChangeIdIsRejected() {
        String base = BODY.replace("call(ChargeCard.class, order);", "if (isVersion(\"charge-v1\", 1)) call(ChargeCard.class, order);");
        String changed = base.replace("call(ChargeCard.class, order);", "call(ChargeCard.class, order);\ncall(SendReceipt.class, order);");
        assertNeedsChangeId(verify(workflow(base), workflow(changed)));
    }

    @Test
    void movingCommandIntoBranchRequiresNewChangeId() {
        String moved = BODY.replace("if (order.isEmpty()) return;", "if (order.isEmpty()) return;\nif (order.isPaid())").replace("call(ChargeCard.class, order);", "    call(ChargeCard.class, order);");
        assertNeedsChangeId(verify(workflow(BODY), workflow(moved)));
    }

    @Test
    void swappedLocalsInConditionRequireNewChangeId() {
        String body = BODY.replace("var order = call(LoadOrder.class, orderId);", "var order = call(LoadOrder.class, orderId);\nvar backup = call(LoadBackup.class, orderId);");
        assertNeedsChangeId(verify(workflow(body), workflow(body.replace("if (order.isEmpty())", "if (backup.isEmpty())"))));
    }

    @Test
    void changedConditionAroundCommandRequiresNewChangeId() {
        String changed = BODY.replace("order.isEmpty()", "order.isEmpty() || order.isPaid()");
        assertNeedsChangeId(verify(workflow(BODY), workflow(changed)));
    }

    @Test
    void safeChangeAnnotationSkipsCosmeticChangesButNotBehaviourChanges() {
        String before = workflow("formatting", BODY);
        assertTrue(verify(before, workflow("formatting", BODY.replace("var order", "final var order"))).isEmpty());
        assertNeedsChangeId(verify(before, workflow("formatting", BODY.replace("order.isEmpty()", "order.isPaid()"))));
    }

    private static Optional<String> verify(String base, String current) {
        return WorkflowCompatibility.verify(Optional.of(base), current);
    }

    private static void assertNeedsChangeId(Optional<String> violation) {
        assertTrue(violation.orElse("").contains("change ID"), () -> "expected a change-ID violation, got: " + violation);
    }

    private static void assertNeedsSafeChange(Optional<String> violation) {
        assertTrue(violation.orElse("").contains("@TemporalSafeChange"), () -> "expected a safe-change violation, got: " + violation);
        assertFalse(violation.orElse("").contains("change ID"));
    }

    private static String workflow(String body) {
        return workflow(null, body);
    }

    private static String workflow(String safeChangeReason, String body) {
        String annotation = safeChangeReason == null ? "" : "@TemporalSafeChange(reason = \"" + safeChangeReason + "\")";
        return """
                package demo;

                %s
                @Workflow
                public class OrderFlow implements BaseWorkflowAction {
                %s
                }
                """.formatted(annotation, body.indent(4).stripTrailing());
    }
}
