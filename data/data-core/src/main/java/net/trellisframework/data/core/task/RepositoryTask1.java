package net.trellisframework.data.core.task;

import net.trellisframework.context.process.Process1;
import net.trellisframework.data.core.data.repository.GenericRepository;

public abstract class RepositoryTask1<TRepository extends GenericRepository<?, ?>, TOutput, TInput> extends BaseRepositoryTask<TRepository> implements Process1<TOutput, TInput> {

    public abstract TOutput execute(TInput t1);

}
