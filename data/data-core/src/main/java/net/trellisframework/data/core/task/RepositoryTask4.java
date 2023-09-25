package net.trellisframework.data.core.task;

import net.trellisframework.context.process.Process4;
import net.trellisframework.data.core.data.repository.GenericRepository;

public interface RepositoryTask4<R extends GenericRepository<?, ?>, O, I1, I2, I3, I4>  extends BaseRepositoryTask<R> , Process4<O, I1, I2, I3, I4> {

    O execute(I1 i1, I2 i2, I3 i3, I4 i4);

}
