package net.trellisframework.data.core.task;

import net.trellisframework.data.core.data.repository.GenericRepository;
import net.trellisframework.context.process.Process3;

public interface RepositoryTask3<R extends GenericRepository, O, I1, I2, I3> extends BaseRepositoryTask<R> , Process3<O, I1, I2, I3> {

    O execute(I1 i1, I2 i2, I3 i3);

}
