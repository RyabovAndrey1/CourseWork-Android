package ru.ryabov.studentperformance.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import ru.ryabov.studentperformance.entity.User;
import ru.ryabov.studentperformance.repository.UserRepository;
import ru.ryabov.studentperformance.service.AuditService;
import ru.ryabov.studentperformance.service.EmailService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
public class WebAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Value("${app.mail.login-notification-enabled:true}")
    private boolean loginNotificationEnabled;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        auditService.logLoginSuccess(authentication.getName(), request.getRemoteAddr());

        // Уведомление о входе отправляется только на email вошедшего пользователя (user.getEmail()).
        if (loginNotificationEnabled && emailService != null) {
            String login = authentication.getName();
            String baseUrl = buildBaseUrl(request);
            String contextPath = request.getContextPath();
            Thread mailThread = new Thread(() -> {
                try {
                    Optional<User> userOpt = userRepository.findByLogin(login);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        String email = user.getEmail();
                        if (email != null && !email.isBlank()) {
                            String logoutUrl = baseUrl + contextPath + "/web/logout";
                            String body = "В ваш аккаунт совершён вход в системе учёта успеваемости.\n\n"
                                    + "Если это были не вы, нажмите на ссылку для выхода из аккаунта:\n" + logoutUrl + "\n\n"
                                    + "— Система учёта успеваемости";
                            emailService.sendMail(email.trim(), "Вход в аккаунт", body);
                        }
                    }
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(WebAuthSuccessHandler.class)
                            .warn("Не удалось отправить уведомление о входе: {}", e.getMessage());
                }
            }, "login-notification-email");
            mailThread.setDaemon(true);
            mailThread.start();
        }

        response.sendRedirect(request.getContextPath() + "/web/dashboard");
    }

    private static String buildBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        if (defaultPort) {
            return scheme + "://" + serverName;
        }
        return scheme + "://" + serverName + ":" + port;
    }
}
