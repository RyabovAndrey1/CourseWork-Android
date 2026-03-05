package ru.ryabov.studentperformance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ServerApplicationTests {

    @Test
    void contextLoads() {
        // Проверка, что контекст Spring загружается успешно
    }

    @Test
    void applicationStarts() {
        // Проверка, что приложение запускается
    }
}
