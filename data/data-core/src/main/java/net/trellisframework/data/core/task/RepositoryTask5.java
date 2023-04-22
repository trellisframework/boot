package net.trellisframework.data.core.task;

import net.trellisframework.data.core.data.repository.GenericRepository;
import net.trellisframework.context.process.Process5;

public abstract class RepositoryTask5<TRepository extends GenericRepository<?, ?>, TOutput, TInput1, TInput2, TInput3, TInput4, TInput5> extends BaseRepositoryTask<TRepository> implements Process5<TOutput, TInput1, TInput2, TInput3, TInput4, TInput5> {

    public abstract TOutput execute(TInput1 t1, TInput2 t2, TInput3 t3, TInput4 t4, TInput5 t5);

}
