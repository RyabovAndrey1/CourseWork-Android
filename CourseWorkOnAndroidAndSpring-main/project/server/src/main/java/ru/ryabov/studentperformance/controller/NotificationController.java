package ru.ryabov.studentperformance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.service.PushNotificationService;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final PushNotificationService pushNotificationService;

    public NotificationController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Регистрация FCM-токена устройства для текущего пользователя.
     * Тело: { "token": "..." }
     */
    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> registerToken(
            @RequestBody(required = false) TokenRequest body,
            Authentication auth) {
        if (body == null || body.getToken() == null || body.getToken().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Укажите token"));
        }
        Long userId = auth != null && auth.getPrincipal() instanceof ru.ryabov.studentperformance.security.UserPrincipal principal
                ? principal.getId()
                : null;
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Не авторизован"));
        }
        pushNotificationService.registerToken(userId, body.getToken().trim());
        return ResponseEntity.ok(ApiResponse.success("Токен зарегистрирован", null));
    }

    public static class TokenRequest {
        private String token;
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}
