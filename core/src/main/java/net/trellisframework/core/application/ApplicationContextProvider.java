package net.trellisframework.core.application;

import net.trellisframework.core.config.ProductionPropertiesDefinition;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EntityScan(basePackages = {"net.trellisframework"})
@ComponentScan(basePackages = "net.trellisframework")
@EnableConfigurationProperties({ProductionPropertiesDefinition.class})
@EnableAspectJAutoProxy
public class ApplicationContextProvider implements ApplicationContextAware {
    public static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

}
