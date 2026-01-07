package net.trellisframework.stream.core.configuration;

import net.trellisframework.stream.core.event.StreamConsumerEvent;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Consumer;

@Configuration
public class StreamConsumerTypeConfiguration implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        for (String beanName : registry.getBeanDefinitionNames()) {
            try {
                BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
                Class<?> beanClass = resolveBeanClass(beanDefinition);
                if (beanClass != null && StreamConsumerEvent.class.isAssignableFrom(beanClass) && beanClass != StreamConsumerEvent.class) {
                    Type genericType = findStreamConsumerEventType(beanClass);
                    if (genericType != null) {
                        ResolvableType resolvableType = ResolvableType.forClassWithGenerics(Consumer.class, ResolvableType.forType(genericType));
                        RootBeanDefinition definition = new RootBeanDefinition(beanClass);
                        definition.setTargetType(resolvableType);
                        definition.setScope(beanDefinition.getScope());
                        definition.setLazyInit(beanDefinition.isLazyInit());
                        definition.setPrimary(beanDefinition.isPrimary());
                        registry.removeBeanDefinition(beanName);
                        registry.registerBeanDefinition(beanName, definition);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private Class<?> resolveBeanClass(BeanDefinition beanDefinition) {
        String bean = beanDefinition.getBeanClassName();
        if (bean != null) {
            try {
                return Class.forName(bean);
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (beanDefinition instanceof AbstractBeanDefinition abstractBeanDefinition) {
            if (abstractBeanDefinition.hasBeanClass()) {
                return abstractBeanDefinition.getBeanClass();
            }
        }
        return null;
    }

    private Type findStreamConsumerEventType(Class<?> clazz) {
        for (Type genericInterface : clazz.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                Type rawType = parameterizedType.getRawType();
                if (rawType == StreamConsumerEvent.class) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0) {
                        return typeArguments[0];
                    }
                }
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return findStreamConsumerEventType(superclass);
        }
        return null;
    }
}
