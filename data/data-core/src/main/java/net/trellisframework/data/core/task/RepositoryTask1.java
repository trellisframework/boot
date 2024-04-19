package net.trellisframework.data.core.task;

import net.trellisframework.context.process.Process1;
import net.trellisframework.data.core.data.repository.GenericRepository;

public interface RepositoryTask1<R extends GenericRepository, O, I1> extends BaseRepositoryTask<R> , Process1<O, I1> {

    O execute(I1 i1);

}
