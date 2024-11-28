package net.trellisframework.boot.cache.core.scanner;

import net.trellisframework.boot.cache.core.annotation.TimeToLive;
import net.trellisframework.boot.cache.core.payload.TTL;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.util.object.ObjectUtil;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AnnotationScanner {

    public static Set<TTL> ttl() {
        Set<TTL> elements = new HashSet<>();
        Map<String, Object> beans = ApplicationContextProvider.context.getBeansWithAnnotation(ComponentScan.class);
        for (Object bean : beans.values()) {
            Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages(bean.getClass().getPackage().getName()).addScanners(Scanners.MethodsAnnotated));
            Set<Method> methods = reflections.getMethodsAnnotatedWith(TimeToLive.class);
            for (Method method : methods) {
                TimeToLive timeToLive = AnnotationUtils.findAnnotation(method, TimeToLive.class);
                Cacheable cacheable = AnnotationUtils.findAnnotation(method, Cacheable.class);
                if (timeToLive != null) {
                    elements.add(TTL.of(ObjectUtil.defaultIfEmpty(timeToLive.value(), Optional.ofNullable(cacheable).map(Cacheable::value).orElse(new String[]{""})), timeToLive.unit(), timeToLive.ttl()));
                }
            }
        }
        return elements;
    }
}
