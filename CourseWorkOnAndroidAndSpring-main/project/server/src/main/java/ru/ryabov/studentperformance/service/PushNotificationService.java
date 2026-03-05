package ru.ryabov.studentperformance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.ryabov.studentperformance.entity.User;
import ru.ryabov.studentperformance.repository.FcmTokenRepository;
import ru.ryabov.studentperformance.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Отправка пуш-уведомлений через FCM (Firebase Cloud Messaging) Legacy API.
 * Если app.fcm.server-key не задан — методы ничего не делают.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private static final String FCM_LEGACY_URL = "https://fcm.googleapis.com/fcm/send";

    private final String serverKey;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public PushNotificationService(
            @Value("${app.fcm.server-key:}") String serverKey,
            FcmTokenRepository fcmTokenRepository,
            UserRepository userRepository) {
        this.serverKey = serverKey != null ? serverKey.trim() : "";
        this.fcmTokenRepository = fcmTokenRepository;
        this.userRepository = userRepository;
    }

    public boolean isEnabled() {
        return !serverKey.isEmpty();
    }

    /** Регистрация FCM-токена для пользователя. */
    public void registerToken(Long userId, String token) {
        if (userId == null || token == null || token.isBlank()) return;
        try {
            fcmTokenRepository.findByUserIdAndToken(userId, token).orElseGet(() -> {
                var entity = new ru.ryabov.studentperformance.entity.FcmToken(userId, token);
                return fcmTokenRepository.save(entity);
            });
        } catch (Exception e) {
            log.warn("Failed to register FCM token for user {}: {}", userId, e.getMessage());
        }
    }

    /** Отправить уведомление одному пользователю. */
    public void sendToUser(Long userId, String title, String body) {
        if (!isEnabled() || userId == null) return;
        List<ru.ryabov.studentperformance.entity.FcmToken> tokens = fcmTokenRepository.findByUserId(userId);
        for (var t : tokens) {
            sendToToken(t.getToken(), title, body);
        }
    }

    /** Отправить уведомление всем пользователям с указанной ролью. */
    public void sendToRole(User.Role role, String title, String body) {
        if (!isEnabled() || role == null) return;
        List<User> users = userRepository.findByRoleAndIsActiveTrue(role);
        List<Long> userIds = users.stream().map(User::getUserId).collect(Collectors.toList());
        if (userIds.isEmpty()) return;
        List<ru.ryabov.studentperformance.entity.FcmToken> tokens = fcmTokenRepository.findByUserIdIn(userIds);
        for (var t : tokens) {
            sendToToken(t.getToken(), title, body);
        }
    }

    private void sendToToken(String token, String title, String body) {
        if (token == null || token.isBlank()) return;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("to", token);
            Map<String, String> notification = new HashMap<>();
            notification.put("title", title != null ? title : "");
            notification.put("body", body != null ? body : "");
            payload.put("notification", notification);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "key=" + serverKey);

            ResponseEntity<String> resp = restTemplate.exchange(
                    FCM_LEGACY_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.warn("FCM send failed: {}", resp.getBody());
            }
        } catch (Exception e) {
            log.warn("FCM send error: {}", e.getMessage());
        }
    }
}
