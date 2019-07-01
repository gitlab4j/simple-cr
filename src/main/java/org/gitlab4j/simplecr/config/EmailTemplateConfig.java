package org.gitlab4j.simplecr.config;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

/**
 * 
 */
@Configuration
public class EmailTemplateConfig {

    public static final String EMAIL_TEMPLATE_ENCODING = StandardCharsets.UTF_8.name();

    @Bean
    public SpringTemplateEngine emailTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(htmlTemplateResolver());
        return templateEngine;
    }

    private ITemplateResolver htmlTemplateResolver() {
        final ClassLoaderTemplateResolver htmlTemplateResolver = new ClassLoaderTemplateResolver();
        htmlTemplateResolver.setResolvablePatterns(Collections.singleton("email/*"));
        htmlTemplateResolver.setPrefix("/templates/");
        htmlTemplateResolver.setSuffix(".html");
        htmlTemplateResolver.setTemplateMode(TemplateMode.HTML);
        htmlTemplateResolver.setCharacterEncoding(EMAIL_TEMPLATE_ENCODING);
        htmlTemplateResolver.setCacheable(false);
        return htmlTemplateResolver;
    }
}