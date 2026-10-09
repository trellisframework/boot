package net.trellisframework.workflow.temporal.logging;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.WorkflowReplayer;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import net.trellisframework.core.log.LogContext;
import net.trellisframework.core.log.Logger;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalLoggerTest {

    private static final String TASK_QUEUE = "temporal-logger-test";
    private TestWorkflowEnvironment environment;
    private final List<LogContext> contexts = new CopyOnWriteArrayList<>();

    @Test
    void resolvingContextOutsideTemporalReturnsEmpty() {
        assertTrue(new TemporalLogContextProvider().resolve().isEmpty());
    }

    @AfterEach
    void tearDown() {
        if (environment != null) environment.close();
        Logger.registerContextProvider(null);
    }

    @Test
    void workflowLoggerIsReplaySafeAndActivityLoggerWorksAcrossRetries() throws Exception {
        TemporalLogContextProvider temporalContextProvider = new TemporalLogContextProvider();
        Logger.registerContextProvider(() -> temporalContextProvider.resolve().map(context -> {
            contexts.add(context);
            return context;
        }));
        LoggingActivityImpl.reset();
        environment = TestWorkflowEnvironment.newInstance();
        Worker worker = environment.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(LoggingWorkflowImpl.class);
        worker.registerActivitiesImplementations(new LoggingActivityImpl());
        environment.start();

        LoggingWorkflow workflow = environment.getWorkflowClient().newWorkflowStub(
                LoggingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId("logger-test-workflow").build());

        assertEquals(2, workflow.run());
        var execution = io.temporal.client.WorkflowStub.fromTyped(workflow).getExecution();
        WorkflowReplayer.replayWorkflowExecution(environment.getWorkflowExecutionHistory(execution), LoggingWorkflowImpl.class);
        assertEquals(2, LoggingActivityImpl.attempts.get());
        assertTrue(contexts.stream().anyMatch(context -> context.value().contains("workflowId=logger-test-workflow")));
        assertTrue(contexts.stream().anyMatch(context -> context.value().contains("activityType=")));
        assertTrue(contexts.stream().anyMatch(LogContext::suppress), "workflow replay logs should be suppressed");
    }

    @WorkflowInterface
    public interface LoggingWorkflow {
        @WorkflowMethod
        int run();
    }

    @net.trellisframework.workflow.temporal.annotation.Workflow
    public static class LoggingWorkflowImpl implements LoggingWorkflow {
        @Override
        public int run() {
            Logger.info("TemporalLoggerTest", "Beginning logger replay test");
            var activity = Workflow.newActivityStub(LoggingActivity.class, ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofMillis(1))
                            .setBackoffCoefficient(1)
                            .setMaximumAttempts(2)
                            .build())
                    .build());
            return activity.run();
        }
    }

    @ActivityInterface
    public interface LoggingActivity {
        @ActivityMethod
        int run();
    }

    @net.trellisframework.workflow.temporal.annotation.Activity
    public static class LoggingActivityImpl implements LoggingActivity {
        private static final AtomicInteger attempts = new AtomicInteger();

        static void reset() {
            attempts.set(0);
        }

        @Override
        public int run() {
            int attempt = Activity.getExecutionContext().getInfo().getAttempt();
            Logger.info("TemporalLoggerTest", "Activity logger attempt %d", attempt);
            attempts.incrementAndGet();
            if (attempt == 1) throw new IllegalStateException("retry once");
            return attempt;
        }
    }
}
