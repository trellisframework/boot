package net.trellisframework.core.application;

import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopProxyUtils;

/**
 * Helper class for self-invocation through proxy.
 * Use this when you need AOP aspects (like @Log) to work on methods called from within the same class.
 * 
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @Service
 * public class MyService {
 *     
 *     public void caller() {
 *         // Instead of: this.targetMethod()
 *         SelfInvoker.self(this, MyService.class).targetMethod();
 *     }
 *     
 *     @Log
 *     public void targetMethod() {
 *         // This will now be logged!
 *     }
 * }
 * }
 * </pre>
 */
public final class SelfInvoker {

    private SelfInvoker() {
    }

    /**
     * Gets the proxy instance of the current bean to enable AOP on self-invocation.
     * 
     * @param target the current instance (usually 'this')
     * @param clazz the class type to cast to
     * @param <T> the type of the bean
     * @return the proxy instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T self(Object target, Class<T> clazz) {
        try {
            return (T) AopContext.currentProxy();
        } catch (IllegalStateException e) {
            // Fallback: try to get from ApplicationContext
            return ApplicationContextProvider.context.getBean(clazz);
        }
    }

    /**
     * Gets the proxy instance of the current bean.
     * 
     * @param <T> the type of the bean
     * @return the proxy instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T self() {
        return (T) AopContext.currentProxy();
    }
}

