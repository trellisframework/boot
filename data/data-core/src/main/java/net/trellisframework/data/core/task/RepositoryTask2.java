package net.trellisframework.data.core.task;

import net.trellisframework.data.core.data.repository.GenericRepository;
import net.trellisframework.context.process.Process2;

public interface RepositoryTask2<R extends GenericRepository<?, ?>, O, I1, I2> extends BaseRepositoryTask<R> , Process2<O, I1, I2> {

    O execute(I1 i1, I2 i2);

}
