package net.trellisframework.workflow.temporal.task;

import net.trellisframework.context.process.Process;
import net.trellisframework.data.core.data.repository.GenericRepository;

public interface WorkflowRepositoryTask<R extends GenericRepository, O> extends BaseWorkflowRepositoryTask<R>, Process<O> {

    O execute();

}

