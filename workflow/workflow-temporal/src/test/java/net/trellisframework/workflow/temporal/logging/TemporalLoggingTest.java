package net.trellisframework.workflow.temporal.logging;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.WorkflowReplayer;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import net.trellisframework.core.log.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalLoggingTest {

    private static final List<LogEvent> EVENTS = new CopyOnWriteArrayList<>();
    private static final AbstractAppender CAPTURE = new AbstractAppender("capture", null, null, false, Property.EMPTY_ARRAY) {
        @Override
        public void append(LogEvent event) {
            EVENTS.add(event.toImmutable());
        }
    };

    @BeforeAll
    static void captureLogs() {
        CAPTURE.start();
        Configurator.setLevel(Logger.class.getName(), Level.INFO);
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(CAPTURE);
        ReplayLogFilter.install();
    }

    @AfterAll
    static void releaseLogs() {
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).removeAppender(CAPTURE);
    }

    @Test
    void temporalContextReachesTheLogEventsAndReplayIsSilent() throws Exception {
        try (TestWorkflowEnvironment environment = environment()) {
            Worker worker = environment.newWorker("logging-test");
            worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);
            worker.registerActivitiesImplementations(new ChargeActivityImpl());
            environment.start();

            OrderWorkflow workflow = environment.getWorkflowClient().newWorkflowStub(OrderWorkflow.class,
                    WorkflowOptions.newBuilder().setTaskQueue("logging-test").setWorkflowId("order-42").build());
            assertEquals(2, workflow.run());

            assertTrue(EVENTS.stream().anyMatch(event -> message(event).contains("charging")
                    && "order-42".equals(event.getContextData().getValue("WorkflowId"))
                    && "Charge".equals(event.getContextData().getValue("ActivityType"))
                    && "2".equals(event.getContextData().getValue("Attempt"))), messages());
            assertTrue(EVENTS.stream().anyMatch(event -> message(event).contains("starting")
                    && "order-42".equals(event.getContextData().getValue("WorkflowId"))), messages());
            long starts = EVENTS.stream().filter(event -> message(event).contains("starting")).count();

            WorkflowReplayer.replayWorkflowExecution(environment.getWorkflowExecutionHistory(WorkflowStub.fromTyped(workflow).getExecution()), worker);
            assertEquals(starts, EVENTS.stream().filter(event -> message(event).contains("starting")).count(), "replay must not log");
        }
    }

    @Test
    void failedWorkflowIsLoggedOnceWithItsDetails() {
        try (TestWorkflowEnvironment environment = environment()) {
            environment.newWorker("failing-test").registerWorkflowImplementationTypes(
                    WorkflowImplementationOptions.newBuilder().setFailWorkflowExceptionTypes(Throwable.class).build(), FailingWorkflowImpl.class);
            environment.start();

            OrderWorkflow workflow = environment.getWorkflowClient().newWorkflowStub(OrderWorkflow.class,
                    WorkflowOptions.newBuilder().setTaskQueue("failing-test").setWorkflowId("order-13").build());
            assertThrows(WorkflowException.class, workflow::run);

            assertEquals(1, EVENTS.stream().filter(event -> message(event).contains("Workflow failed: java.lang.IllegalStateException")
                    && "order-13".equals(event.getContextData().getValue("WorkflowId"))).count(), messages());
        }
    }

    @Test
    void correlationPatternPrintsOnlyWhatIsPresent() {
        PatternLayout layout = PatternLayout.newBuilder().withPattern(TemporalLogCorrelation.PATTERN).build();
        assertEquals("[OrderWorkflow order-42] [Charge#2] ", layout.toSerializable(event(Map.of("WorkflowType", "OrderWorkflow", "WorkflowId", "order-42", "ActivityType", "Charge", "Attempt", "2"))));
        assertEquals("[OrderWorkflow order-42] ", layout.toSerializable(event(Map.of("WorkflowType", "OrderWorkflow", "WorkflowId", "order-42"))));
        assertEquals("", layout.toSerializable(event(Map.of())));
    }

    @Test
    void correlationPatternIsAddedToTheEnvironmentAfterAnyConfiguredOne() {
        MockEnvironment environment = new MockEnvironment();
        new TemporalLogCorrelation().postProcessEnvironment(environment, null);
        assertEquals(TemporalLogCorrelation.PATTERN, environment.getProperty(TemporalLogCorrelation.PROPERTY));

        MockEnvironment traced = new MockEnvironment().withProperty(TemporalLogCorrelation.PROPERTY, "[%X{traceId}] ");
        new TemporalLogCorrelation().postProcessEnvironment(traced, null);
        assertEquals("[%X{traceId}] " + TemporalLogCorrelation.PATTERN, traced.getProperty(TemporalLogCorrelation.PROPERTY));
    }

    private static TestWorkflowEnvironment environment() {
        return TestWorkflowEnvironment.newInstance(TestEnvironmentOptions.newBuilder()
                .setWorkerFactoryOptions(WorkerFactoryOptions.newBuilder().setWorkerInterceptors(new TemporalWorkerInterceptor()).build())
                .build());
    }

    private static LogEvent event(Map<String, String> context) {
        StringMap data = new SortedArrayStringMap();
        context.forEach(data::putValue);
        return Log4jLogEvent.newBuilder().setMessage(new SimpleMessage("m")).setContextData(data).build();
    }

    private static String message(LogEvent event) {
        return event.getMessage().getFormattedMessage();
    }

    private static String messages() {
        return EVENTS.stream().map(TemporalLoggingTest::message).toList().toString();
    }

    @WorkflowInterface
    public interface OrderWorkflow {
        @WorkflowMethod
        int run();
    }

    public static class OrderWorkflowImpl implements OrderWorkflow {
        @Override
        public int run() {
            Logger.info("OrderWorkflow", "starting");
            return Workflow.newActivityStub(ChargeActivity.class, ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder().setInitialInterval(Duration.ofMillis(1)).setBackoffCoefficient(1).setMaximumAttempts(2).build())
                    .build()).charge();
        }
    }

    public static class FailingWorkflowImpl implements OrderWorkflow {
        @Override
        public int run() {
            throw new IllegalStateException("broken");
        }
    }

    @ActivityInterface
    public interface ChargeActivity {
        @ActivityMethod
        int charge();
    }

    public static class ChargeActivityImpl implements ChargeActivity {
        @Override
        public int charge() {
            int attempt = Activity.getExecutionContext().getInfo().getAttempt();
            Logger.info("ChargeActivity", "charging");
            if (attempt == 1)
                throw new IllegalStateException("retry once");
            return attempt;
        }
    }
}
