package ru.ryabov.studentperformance.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Создаёт JavaMailSender, если почта включена и задан spring.mail.host.
 * При профиле H2 по умолчанию почта отключена (spring.mail.enabled=false).
 * Чтобы включить: профиль h2,local + application-local.yml с паролем и spring.mail.enabled=true.
 */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "true")
    @ConditionalOnExpression("@environment.getProperty('spring.mail.host') != null && !@environment.getProperty('spring.mail.host').isBlank()")
    public JavaMailSender javaMailSender(
            @org.springframework.beans.factory.annotation.Value("${spring.mail.host}") String host,
            @org.springframework.beans.factory.annotation.Value("${spring.mail.port:587}") int port,
            @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}") String username,
            @org.springframework.beans.factory.annotation.Value("${spring.mail.password:}") String password
    ) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        Properties props = new Properties();
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.starttls.enable", "true");
        props.setProperty("mail.smtp.connectiontimeout", "10000");
        props.setProperty("mail.smtp.timeout", "10000");
        props.setProperty("mail.smtp.writetimeout", "10000");
        if (host != null && host.toLowerCase().contains("gmail")) {
            props.setProperty("mail.smtp.ssl.trust", "smtp.gmail.com");
        }
        sender.setJavaMailProperties(props);
        return sender;
    }
}
