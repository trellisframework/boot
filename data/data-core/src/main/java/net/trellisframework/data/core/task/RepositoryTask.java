package net.trellisframework.data.core.task;

import net.trellisframework.data.core.data.repository.GenericRepository;
import net.trellisframework.context.process.Process;

public abstract class RepositoryTask<TRepository extends GenericRepository<?, ?>, TOutput> extends BaseRepositoryTask<TRepository> implements Process<TOutput> {

    public abstract TOutput execute();

}
