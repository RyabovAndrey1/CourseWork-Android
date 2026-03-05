package ru.ryabov.studentperformance.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Утилита для получения текущего пользователя из контекста безопасности (для аудита и др.).
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /** Логин текущего пользователя или "anonymous" / "system" если не аутентифицирован. */
    public static String getCurrentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "anonymous";
        }
        return auth.getName();
    }
}
