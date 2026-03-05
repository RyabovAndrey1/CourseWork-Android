package ru.ryabov.studentperformance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import ru.ryabov.studentperformance.repository.FacultyRepository;

/**
 * Инициализация данных для профиля h2.
 */
@Configuration
@Profile("h2")
public class H2DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(H2DataInitializer.class);

    @Bean
    @Order(1)
    ApplicationRunner initH2Data(FacultyRepository facultyRepo) {
        return args -> {
            // no-op (данные не заполняются автоматически)
            log.info("H2: auto-init отключён (данные не заполняются автоматически). Faculty count={}", facultyRepo.count());
        };
    }
}
