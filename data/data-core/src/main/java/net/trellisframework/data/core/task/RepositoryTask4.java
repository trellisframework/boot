package net.trellisframework.data.core.task;

import net.trellisframework.context.process.Process4;
import net.trellisframework.data.core.data.repository.GenericRepository;

public abstract class RepositoryTask4<TRepository extends GenericRepository<?, ?>, TOutput, TInput1, TInput2, TInput3, TInput4>  extends BaseRepositoryTask<TRepository> implements Process4<TOutput, TInput1, TInput2, TInput3, TInput4> {

    public abstract TOutput execute(TInput1 t1, TInput2 t2, TInput3 t3, TInput4 t4);

}
