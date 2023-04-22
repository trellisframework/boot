package net.trellisframework.data.core.task;

import net.trellisframework.data.core.data.repository.GenericRepository;
import net.trellisframework.context.process.Process2;

public abstract class RepositoryTask2<TRepository extends GenericRepository<?, ?>, TOutput, TInput1, TInput2> extends BaseRepositoryTask<TRepository> implements Process2<TOutput, TInput1, TInput2> {

    public abstract TOutput execute(TInput1 t1, TInput2 t2);

}
