package net.trellisframework.workflow.temporal.task;

import net.trellisframework.context.process.Process5;
import net.trellisframework.data.core.data.repository.GenericRepository;

public interface WorkflowRepositoryTask5<R extends GenericRepository, O, I1, I2, I3, I4, I5> extends BaseWorkflowRepositoryTask<R>, Process5<O, I1, I2, I3, I4, I5> {

    O execute(I1 i1, I2 i2, I3 i3, I4 i4, I5 i5);

}

