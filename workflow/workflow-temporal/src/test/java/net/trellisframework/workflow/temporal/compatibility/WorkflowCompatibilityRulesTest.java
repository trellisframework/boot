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
    void shouldIgnoreFilesWithoutWorkflowClass() {
        String plain = "class Helper { void run(String id) { call(LoadOrder.class, id); } }";
        assertTrue(verify(plain, plain.replace("id)", "id) { }")).isEmpty());
    }

    @Test
    void shouldAcceptNewWorkflow() {
        assertTrue(WorkflowCompatibility.verify(Optional.empty(), workflow(BODY)).isEmpty());
    }

    @Test
    void shouldRequireSafeChangeForRenamedLocalButAcceptExplicitAnnotation() {
        String renamed = BODY.replace("order", "loaded");
        assertNeedsSafeChange(verify(workflow(BODY), workflow(renamed)));
        assertTrue(verify(workflow(BODY), workflow("locals renamed", renamed)).isEmpty());
    }

    @Test
    void shouldRequireSafeChangeForPlainStatementsAroundCommands() {
        String logged = BODY.replace("if (order.isEmpty()) return;", "log.info(\"loaded {}\", order);\nif (order.isEmpty()) return;");
        assertNeedsSafeChange(verify(workflow(BODY), workflow(logged)));
    }

    @Test
    void shouldRequireNewChangeIdForAddedCommand() {
        String added = BODY.replace("call(ChargeCard.class, order);", "call(ChargeCard.class, order);\ncall(SendReceipt.class, order);");
        assertNeedsChangeId(verify(workflow(BODY), workflow(added)));
        assertNeedsChangeId(verify(workflow(BODY), workflow("just a receipt", added)));
    }

    @Test
    void shouldAcceptAddedCommandGuardedByNewChangeId() {
        String versioned = BODY.replace("call(ChargeCard.class, order);",
                "call(ChargeCard.class, order);\nif (isVersion(\"receipt-v1\", 1)) call(SendReceipt.class, order);");
        assertTrue(verify(workflow(BODY), workflow(versioned)).isEmpty());
    }

    @Test
    void shouldRejectReusingExistingChangeId() {
        String base = BODY.replace("call(ChargeCard.class, order);", "if (isVersion(\"charge-v1\", 1)) call(ChargeCard.class, order);");
        String changed = base.replace("call(ChargeCard.class, order);", "call(ChargeCard.class, order);\ncall(SendReceipt.class, order);");
        assertNeedsChangeId(verify(workflow(base), workflow(changed)));
    }

    @Test
    void shouldAcceptRaisedMaxVersionOnExistingChangeId() {
        String base = BODY.replace("call(ChargeCard.class, order);", "if (isVersion(\"charge-v1\", 1)) call(ChargeCard.class, order);");
        String changed = base.replace(
                "if (isVersion(\"charge-v1\", 1)) call(ChargeCard.class, order);",
                "if (isVersion(\"charge-v1\", 2)) call(SendReceipt.class, order);");
        assertTrue(verify(workflow(base), workflow(changed)).isEmpty());
    }

    @Test
    void shouldRejectCopyOfExistingVersionCallAsNewGuard() {
        String base = BODY.replace("call(ChargeCard.class, order);", "if (isVersion(\"charge-v1\", 1)) call(ChargeCard.class, order);");
        String changed = base.replace("call(ChargeCard.class, order);", "call(ChargeCard.class, order);\nif (isVersion(\"charge-v1\", 1)) call(SendReceipt.class, order);");
        assertNeedsChangeId(verify(workflow(base), workflow(changed)));
    }

    @Test
    void shouldRejectRenamedExistingChangeIdEvenWithNewVersionCall() {
        String base = BODY.replace("call(ChargeCard.class, order);",
                "if (isVersion(\"charge-v1\", 1)) call(ChargeCard.class, order);");
        String changed = base.replace("charge-v1", "charge-v2")
                .replace("call(ChargeCard.class, order);",
                        "call(ChargeCard.class, order);\nif (isVersion(\"receipt-v1\", 1)) call(SendReceipt.class, order);");
        assertNeedsChangeId(verify(workflow(base), workflow(changed)));
    }

    @Test
    void shouldRequireNewChangeIdForChangedTemporalCommandInsideHelper() {
        String base = workflow("""
                public void run(String orderId) {
                    persist(orderId);
                }

                private void persist(String orderId) {
                    call(ChargeCard.class, orderId);
                }
                """);
        String changed = base.replace("call(ChargeCard.class, orderId);",
                "call(SendReceipt.class, orderId);");
        assertNeedsChangeId(verify(base, changed));
    }

    @Test
    void shouldAllowHelperChangeWithVersionCallInsideHelper() {
        String base = workflow("""
                public void run(String orderId) {
                    persist(orderId);
                }

                private void persist(String orderId) {
                    call(ChargeCard.class, orderId);
                }
                """);
        String changed = base.replace("call(ChargeCard.class, orderId);",
                "if (isVersion(\"receipt-v1\", 1)) call(SendReceipt.class, orderId);");
        assertTrue(verify(base, changed).isEmpty());
    }

    @Test
    void shouldRequireNewChangeIdForChangedTemporalCommandInput() {
        String base = workflow("""
                public void run() {
                    call(SaveTask.class, "before");
                }
                """);
        String changed = base.replace("\"before\"", "\"after\"");
        assertNeedsChangeId(verify(base, changed));
    }

    @Test
    void shouldRequireNewChangeIdForChangedWorkflowSleep() {
        String base = workflow("""
                public void run() {
                    Workflow.sleep(Duration.ofSeconds(1));
                }
                """);
        String changed = base.replace("ofSeconds(1)", "ofSeconds(2)");
        assertNeedsChangeId(verify(base, changed));
    }

    @Test
    void shouldRequireNewChangeIdForChangedCallAsync() {
        String base = workflow("""
                public void run() {
                    callAsync(SaveTask.class, "before");
                }
                """);
        String changed = base.replace("SaveTask.class", "UpdateTask.class");
        assertNeedsChangeId(verify(base, changed));
    }

    @Test
    void shouldRejectChangedGetVersionChangeId() {
        String base = workflow("""
                public void run() {
                    Workflow.getVersion("flow-v1", 1, 1);
                }
                """);
        String changed = base.replace("flow-v1", "flow-v2");
        assertNeedsChangeId(verify(base, changed));
    }

    @Test
    void shouldAllowHelperLocalRenameOnlyWithExplicitAnnotation() {
        String base = workflow("""
                public void run(String orderId) {
                    persist(orderId);
                }

                private void persist(String orderId) {
                    var order = call(LoadOrder.class, orderId);
                    call(ChargeCard.class, order);
                }
                """);
        String renamed = base.replace("var order", "var loaded")
                .replace("ChargeCard.class, order", "ChargeCard.class, loaded");

        assertNeedsSafeChange(verify(base, renamed));
        assertTrue(verify(base, renamed.replace("@Workflow", "@TemporalSafeChange(reason = \"Renamed helper local\")\n@Workflow")).isEmpty());
    }

    @Test
    void shouldNotAllowBlankTemporalSafeChangeReasonToBypassValidation() {
        String base = workflow("""
                public void run() {
                    call(SaveTask.class, "before");
                }
                """);
        String changed = workflow("", """
                public void run() {
                    final var value = call(SaveTask.class, "before");
                }
                """);
        assertNeedsSafeChange(verify(base, changed));
    }

    @Test
    void shouldRejectUnsupportedReplaySafeChangeAnnotation() {
        String base = workflow("""
                public void run() {
                    var value = call(SaveTask.class, "before");
                }
                """);
        String changed = workflow("safe", """
                public void run() {
                    final var value = call(SaveTask.class, "before");
                }
                """).replace("@TemporalSafeChange", "@ReplaySafeChange");

        assertNeedsSafeChange(verify(base, changed));
    }

    @Test
    void shouldRequireNewChangeIdWhenCommandMovesIntoBranch() {
        String moved = BODY.replace("if (order.isEmpty()) return;", "if (order.isEmpty()) return;\nif (order.isPaid())").replace("call(ChargeCard.class, order);", "    call(ChargeCard.class, order);");
        assertNeedsChangeId(verify(workflow(BODY), workflow(moved)));
    }

    @Test
    void shouldRequireNewChangeIdForSwappedLocalsInCondition() {
        String body = BODY.replace("var order = call(LoadOrder.class, orderId);", "var order = call(LoadOrder.class, orderId);\nvar backup = call(LoadBackup.class, orderId);");
        assertNeedsChangeId(verify(workflow(body), workflow(body.replace("if (order.isEmpty())", "if (backup.isEmpty())"))));
    }

    @Test
    void shouldRequireNewChangeIdForChangedConditionAroundCommand() {
        String changed = BODY.replace("order.isEmpty()", "order.isEmpty() || order.isPaid()");
        assertNeedsChangeId(verify(workflow(BODY), workflow(changed)));
    }

    @Test
    void shouldAllowAnnotatedCosmeticChangesButRejectBehaviourChanges() {
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
