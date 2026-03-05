package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.auth.AuthRequest;
import ru.ryabov.studentperformance.dto.auth.AuthResponse;
import ru.ryabov.studentperformance.dto.auth.ChangePasswordRequest;
import ru.ryabov.studentperformance.dto.auth.ForgotPasswordRequest;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.AuditService;
import ru.ryabov.studentperformance.service.AuthService;
import ru.ryabov.studentperformance.service.EmailService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired(required = false)
    private EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.login(request);
            auditService.logLoginSuccess(request.getLogin(), httpRequest.getRemoteAddr());
            return ResponseEntity.ok(ApiResponse.success("Авторизация успешна", response));
        } catch (Exception e) {
            auditService.logLoginFailure(request.getLogin(), e.getMessage(), httpRequest.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<AuthResponse>error(e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody AuthRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.ok(ApiResponse.<Void>success("Пользователь зарегистрирован", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.<Void>success("Вы вышли из системы", null));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<String>> checkAuth(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(ApiResponse.success("Авторизован", authentication.getName()));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<String>error("Не авторизован"));
    }

    /** Запрос сброса пароля (без авторизации). Отправляет на email ссылку для сброса. */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String email = request != null && request.getEmail() != null ? request.getEmail().trim() : "";
        if (email.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Укажите email"));
        }
        java.util.Optional<String> tokenOpt = authService.requestPasswordReset(email);
        if (tokenOpt.isPresent()) {
            try {
                String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName()
                        + (httpRequest.getServerPort() == 80 || httpRequest.getServerPort() == 443 ? "" : ":" + httpRequest.getServerPort())
                        + httpRequest.getContextPath();
                String resetLink = baseUrl + "/web/reset-password?token=" + tokenOpt.get();
                if (emailService != null) {
                    emailService.sendPasswordResetLink(email, resetLink);
                }
            } catch (Exception ignored) { /* не раскрываем ошибку */ }
        }
        return ResponseEntity.ok(ApiResponse.success("Если указанный email зарегистрирован, на него отправлено письмо с инструкцией по восстановлению пароля.", null));
    }

    /** Смена пароля (для мобильного приложения; требуется JWT). */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.<Void>error("Требуется авторизация"));
        }
        try {
            authService.changePassword(principal.getId(), request.getOldPassword(), request.getNewPassword());
            auditService.logChangePassword(principal.getUsername(), true, httpRequest.getRemoteAddr());
            return ResponseEntity.ok(ApiResponse.<Void>success("Пароль изменён", null));
        } catch (Exception e) {
            auditService.logChangePassword(principal.getUsername(), false, httpRequest.getRemoteAddr());
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error(e.getMessage()));
        }
    }
}
