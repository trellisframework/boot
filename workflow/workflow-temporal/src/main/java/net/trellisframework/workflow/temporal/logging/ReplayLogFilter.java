package net.trellisframework.workflow.temporal.logging;

import io.temporal.workflow.unsafe.WorkflowUnsafe;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.AbstractFilter;

public final class ReplayLogFilter extends AbstractFilter {

    public static void install() {
        LoggerContext context = LoggerContext.getContext(false);
        context.getConfiguration().getRootLogger().addFilter(new ReplayLogFilter());
        context.updateLoggers();
    }

    @Override
    public Result filter(LogEvent event) {
        return WorkflowUnsafe.isWorkflowThread() && WorkflowUnsafe.isReplaying() ? Result.DENY : Result.NEUTRAL;
    }
}
