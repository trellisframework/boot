package net.trellisframework.context.provider;

import net.trellisframework.context.process.BaseProcess;
import net.trellisframework.core.application.ApplicationContextProvider;
import org.apache.commons.collections4.map.LRUMap;
import org.mockito.Mockito;

import java.util.Optional;

public interface InjectorBeanProvider {

    LRUMap<Class<?>, Object> DI = new LRUMap<>(10000);

    default <T extends BaseProcess> void inject(T... values) {
        for (T value : values) {
            DI.put(Mockito.mockingDetails(value).isMock() ?
                    Mockito.mockingDetails(value).getMockCreationSettings().getTypeToMock() :
                    value.getClass() , value);
        }
    }

    default <T> T getBean(Class<T> clazz) {
        return Optional.ofNullable((T) DI.get(clazz)).orElse(ApplicationContextProvider.context.getBean(clazz));
    }
}
