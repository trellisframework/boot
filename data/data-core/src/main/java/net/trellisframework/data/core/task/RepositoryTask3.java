package net.trellisframework.data.core.task;

import net.trellisframework.data.core.data.repository.GenericRepository;
import net.trellisframework.context.process.Process3;

public abstract class RepositoryTask3<TRepository extends GenericRepository<?, ?>, TOutput, TInput1, TInput2, TInput3> extends BaseRepositoryTask<TRepository> implements Process3<TOutput, TInput1, TInput2, TInput3> {

    public abstract TOutput execute(TInput1 t1, TInput2 t2, TInput3 t3);

}
