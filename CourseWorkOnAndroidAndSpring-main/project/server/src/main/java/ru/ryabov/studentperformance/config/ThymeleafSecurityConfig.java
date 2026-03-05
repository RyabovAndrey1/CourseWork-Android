package ru.ryabov.studentperformance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

/**
 * Регистрация диалекта Thymeleaf для Spring Security (sec:authorize в шаблонах).
 */
@Configuration
public class ThymeleafSecurityConfig {

    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }
}
