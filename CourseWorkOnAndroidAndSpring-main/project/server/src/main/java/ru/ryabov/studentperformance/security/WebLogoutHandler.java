package ru.ryabov.studentperformance.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import ru.ryabov.studentperformance.service.AuditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Логирование выхода пользователя (аудит).
 */
@Component
public class WebLogoutHandler implements LogoutHandler {

    @Autowired
    private AuditService auditService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            auditService.logLogout(authentication.getName());
        }
    }
}
