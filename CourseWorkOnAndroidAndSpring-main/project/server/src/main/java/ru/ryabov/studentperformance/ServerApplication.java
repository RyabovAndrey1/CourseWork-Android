package ru.ryabov.studentperformance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/** Запуск только компонентов studentperformance (исключаем ru.ryabov.controller от другого проекта). */
@SpringBootApplication(exclude = MailSenderAutoConfiguration.class)
@ComponentScan(basePackages = "ru.ryabov.studentperformance")
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
