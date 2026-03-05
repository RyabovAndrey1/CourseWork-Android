package ru.ryabov.studentperformance.service;

import ru.ryabov.studentperformance.dto.auth.AuthRequest;
import ru.ryabov.studentperformance.dto.auth.AuthResponse;

public interface AuthService {

    AuthResponse login(AuthRequest request);

    void register(AuthRequest request);

    void changePassword(Long userId, String oldPassword, String newPassword);

    /** Смена пароля по логину (для формы на странице входа без авторизации). */
    void changePasswordByLogin(String login, String oldPassword, String newPassword);

    /** Запросить сброс пароля: создаётся токен. Возвращает токен для формирования ссылки (если пользователь с таким email найден). */
    java.util.Optional<String> requestPasswordReset(String email);

    /** Установить новый пароль по токену из ссылки (токен после использования удаляется). Возвращает email пользователя для уведомления. */
    java.util.Optional<String> resetPasswordByToken(String token, String newPassword);

    /** Email пользователя по логину (для отправки уведомления после смены пароля). */
    java.util.Optional<String> findEmailByLogin(String login);
}
