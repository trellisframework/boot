package net.trellisframework.workflow.temporal.task;

import net.trellisframework.context.process.Process2;
import net.trellisframework.data.core.data.repository.GenericRepository;

public interface WorkflowRepositoryTask2<R extends GenericRepository, O, I1, I2> extends BaseWorkflowRepositoryTask<R>, Process2<O, I1, I2> {

    O execute(I1 i1, I2 i2);

}

