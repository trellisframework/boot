package net.trellisframework.workflow.temporal.compatibility;

import org.jetbrains.annotations.NotNull;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

public final class WorkflowCompatibilityEngine implements TestEngine {

    public static final String ID = "workflow-compatibility";
    public static final String SKIP_PROPERTY = "workflow.skip";

    private static final Class<?> SUITE = WorkflowCompatibility.class;

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public TestDescriptor discover(@NotNull EngineDiscoveryRequest request, @NotNull UniqueId uniqueId) {
        TestDescriptor engine = new EngineDescriptor(uniqueId, "Workflow compatibility");
        TestDescriptor suite = descriptor(uniqueId.append("class", SUITE.getName()), SUITE.getSimpleName(), ClassSource.from(SUITE), TestDescriptor.Type.CONTAINER);
        suite.addChild(descriptor(suite.getUniqueId().append("method", "verify"), "changed workflows are versioned or marked safe",
                MethodSource.from(SUITE.getName(), "verify"), TestDescriptor.Type.TEST));
        engine.addChild(suite);
        return engine;
    }

    @Override
    public void execute(ExecutionRequest request) {
        EngineExecutionListener listener = request.getEngineExecutionListener();
        TestDescriptor engine = request.getRootTestDescriptor();
        listener.executionStarted(engine);
        for (TestDescriptor suite : engine.getChildren()) {
            listener.executionStarted(suite);
            suite.getChildren().forEach(test -> run(test, listener));
            listener.executionFinished(suite, TestExecutionResult.successful());
        }
        listener.executionFinished(engine, TestExecutionResult.successful());
    }

    private static void run(TestDescriptor test, EngineExecutionListener listener) {
        if (Boolean.getBoolean(SKIP_PROPERTY)) {
            listener.executionSkipped(test, "-D" + SKIP_PROPERTY + "=true");
            return;
        }
        listener.executionStarted(test);
        try {
            WorkflowCompatibility.verify();
            listener.executionFinished(test, TestExecutionResult.successful());
        } catch (AssertionError e) {
            listener.executionFinished(test, TestExecutionResult.failed(e));
        } catch (RuntimeException e) {
            listener.executionFinished(test, TestExecutionResult.aborted(e));
        }
    }

    private static TestDescriptor descriptor(UniqueId uniqueId, String displayName, TestSource source, TestDescriptor.Type type) {
        return new AbstractTestDescriptor(uniqueId, displayName, source) {
            @NotNull
            @Override
            public Type getType() {
                return type;
            }
        };
    }
}
