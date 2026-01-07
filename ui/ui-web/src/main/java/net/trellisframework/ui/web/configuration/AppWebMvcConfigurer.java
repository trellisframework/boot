package net.trellisframework.ui.web.configuration;

import net.trellisframework.context.validator.FluentValidatorFactoryBean;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppWebMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addFormatters(@NotNull FormatterRegistry registry) {
        ApplicationConversionService.configure(registry);
    }

    @Override
    public Validator getValidator() {
        return new FluentValidatorFactoryBean();
    }

}