package net.trellisframework.data.core.task;

import net.trellisframework.context.process.Process;
import net.trellisframework.data.core.data.repository.GenericRepository;

public interface RepositoryTask<R extends GenericRepository<?, ?>, O> extends BaseRepositoryTask<R>, Process<O> {

    O execute();

}
