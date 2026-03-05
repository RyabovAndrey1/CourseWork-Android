package ru.ryabov.studentperformance.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import ru.ryabov.studentperformance.service.AuditService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class WebAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private AuditService auditService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws ServletException, IOException {
        String login = request.getParameter("username");
        auditService.logLoginFailure(login != null ? login : "?", exception.getMessage(), request.getRemoteAddr());
        setDefaultFailureUrl("/web/login?error=true");
        super.onAuthenticationFailure(request, response, exception);
    }
}
