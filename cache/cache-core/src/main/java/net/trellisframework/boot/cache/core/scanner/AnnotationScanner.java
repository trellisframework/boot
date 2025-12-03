package net.trellisframework.boot.cache.core.scanner;

import net.trellisframework.boot.cache.core.constant.CacheSerializer;
import net.trellisframework.boot.cache.core.payload.CacheableConfig;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.util.object.ObjectUtil;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AnnotationScanner {

    public static Set<CacheableConfig> cacheableConfig() {
        Set<CacheableConfig> elements = new HashSet<>();

        Map<String, Object> beans = ApplicationContextProvider.context.getBeansWithAnnotation(ComponentScan.class);
        for (Object bean : beans.values()) {
            Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages(bean.getClass().getPackage().getName()).addScanners(Scanners.MethodsAnnotated));
            Set<Method> methods = reflections.getMethodsAnnotatedWith(net.trellisframework.boot.cache.core.annotation.CacheableConfig.class);
            for (Method method : methods) {
                net.trellisframework.boot.cache.core.annotation.CacheableConfig config = AnnotationUtils.findAnnotation(method, net.trellisframework.boot.cache.core.annotation.CacheableConfig.class);
                Cacheable cacheable = AnnotationUtils.findAnnotation(method, Cacheable.class);
                if (config != null) {
                    elements.add(CacheableConfig.of(ObjectUtil.defaultIfEmpty(config.value(), Optional.ofNullable(cacheable).map(Cacheable::value).orElse(new String[]{""})), Optional.ofNullable(config.ttl()).map(DurationStyle.SIMPLE::parse).orElse(null), Optional.ofNullable(config.serializer()).orElse(CacheSerializer.JDK)));
                }
            }
        }
        return elements;
    }
}
