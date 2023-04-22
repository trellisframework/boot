package net.trellisframework.data.core.task;

import net.trellisframework.context.task.BaseTask;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.data.core.data.repository.GenericRepository;
import net.trellisframework.data.core.util.PagingModelMapper;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.ParameterizedType;

@Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRES_NEW,
        rollbackFor = {TransactionSystemException.class}
)
public abstract class BaseRepositoryTask<TRepository extends GenericRepository<?, ?>> extends BaseTask implements PagingModelMapper {

    private TRepository repository;

    public void inject(TRepository repository) {
        this.repository = repository;
    }

    public TRepository getRepository() {
        return repository == null ?
                ApplicationContextProvider.context.getBean((Class<TRepository>) ((ParameterizedType) (getClass().getGenericSuperclass())).getActualTypeArguments()[0]) :
                repository;
    }


}
