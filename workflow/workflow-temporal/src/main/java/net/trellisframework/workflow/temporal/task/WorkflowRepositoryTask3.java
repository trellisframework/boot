package net.trellisframework.workflow.temporal.task;

import net.trellisframework.context.process.Process3;
import net.trellisframework.data.core.data.repository.GenericRepository;

public interface WorkflowRepositoryTask3<R extends GenericRepository, O, I1, I2, I3> extends BaseWorkflowRepositoryTask<R>, Process3<O, I1, I2, I3> {

    O execute(I1 i1, I2 i2, I3 i3);

}

