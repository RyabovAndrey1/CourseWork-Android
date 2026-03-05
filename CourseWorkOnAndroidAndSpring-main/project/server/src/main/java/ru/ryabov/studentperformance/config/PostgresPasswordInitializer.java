package ru.ryabov.studentperformance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.ryabov.studentperformance.repository.UserRepository;

import java.util.List;

/**
 * Для профиля postgres: при старте обновляет пароль тестовых пользователей на password123,
 * т.к. в Flyway-миграциях может быть старый хеш.
 * При временной недоступности БД (например, «Connection reset by peer» из-за пула) — повторяет попытку и не роняет старт приложения.
 */
@Component
@Profile("postgres")
public class PostgresPasswordInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PostgresPasswordInitializer.class);
    private static final String SEED_PASSWORD = "password123";
    private static final List<String> LOGINS = List.of(
            "admin", "teacher1", "teacher2", "student1", "student2", "student3", "deanery1"
    );
    private static final int MAX_ATTEMPTS = 3;
    private static final long DELAY_MS = 2000;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                doRun();
                return;
            } catch (DataAccessException e) {
                log.warn("PostgresPasswordInitializer попытка {}/{} не удалась: {}", attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("PostgresPasswordInitializer прерван");
                        return;
                    }
                } else {
                    log.error("PostgresPasswordInitializer не смог обновить пароли после {} попыток. Приложение продолжит работу; при первом входе пароли могут быть старыми.", MAX_ATTEMPTS);
                }
            }
        }
    }

    @Transactional
    protected void doRun() {
        String encoded = passwordEncoder.encode(SEED_PASSWORD);
        for (String login : LOGINS) {
            userRepository.findByLogin(login).ifPresent(u -> {
                u.setPasswordHash(encoded);
                userRepository.saveAndFlush(u);
            });
        }
    }
}
