package ru.ryabov.studentperformance.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ryabov.studentperformance.dto.auth.AuthRequest;
import ru.ryabov.studentperformance.dto.auth.AuthResponse;
import ru.ryabov.studentperformance.entity.PasswordResetToken;
import ru.ryabov.studentperformance.entity.User;
import ru.ryabov.studentperformance.entity.User.Role;
import ru.ryabov.studentperformance.repository.PasswordResetTokenRepository;
import ru.ryabov.studentperformance.repository.UserRepository;
import ru.ryabov.studentperformance.security.JwtService;
import ru.ryabov.studentperformance.service.AuthService;
import ru.ryabov.studentperformance.service.EmailService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired(required = false)
    private EmailService emailService;

    private static final int RESET_TOKEN_VALIDITY_HOURS = 1;
    private static final SecureRandom RND = new SecureRandom();

    @Override
    @Transactional
    public AuthResponse login(AuthRequest request) {
        // Аутентификация пользователя
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
        );

        // Поиск пользователя
        User user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверка активности
        if (!user.getIsActive()) {
            throw new RuntimeException("Учетная запись деактивирована");
        }

        // Генерация JWT токена
        String token = jwtService.generateToken(user);

        // Уведомление о входе только на email этого пользователя (не на другие адреса).
        if (emailService != null && emailService.isMailConfigured()
                && user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.sendMail(user.getEmail().trim(), "Вход в аккаунт — Учёт успеваемости",
                        "Вы вошли в систему контроля успеваемости.\n\nЕсли это были не вы, рекомендуем срочно сменить пароль в приложении (раздел «Профиль» → «Изменить пароль») или восстановить доступ по ссылке «Забыли пароль?» на экране входа.");
            } catch (Exception ignored) { /* не ломаем логин из-за почты */ }
        }

        return new AuthResponse(
                token,
                user.getUserId(),
                user.getLogin(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }

    @Override
    @Transactional
    public void register(AuthRequest request) {
        // Проверка уникальности логина
        if (userRepository.existsByLogin(request.getLogin())) {
            throw new RuntimeException("Логин уже используется");
        }

        // Проверка уникальности email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email уже используется");
        }

        // Определение роли (по умолчанию STUDENT)
        Role role = Role.STUDENT;
        if (request.getRole() != null) {
            role = Role.valueOf(request.getRole());
        }

        // Создание пользователя
        User user = new User(
                request.getLogin(),
                passwordEncoder.encode(request.getPassword()),
                request.getEmail(),
                request.getLastName(),
                request.getFirstName(),
                request.getMiddleName(),
                role
        );

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверка старого пароля
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Неверный текущий пароль");
        }

        // Установка нового пароля
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePasswordByLogin(String login, String oldPassword, String newPassword) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Пользователь с таким логином не найден"));
        changePassword(user.getUserId(), oldPassword, newPassword);
    }

    @Override
    @Transactional
    public java.util.Optional<String> requestPasswordReset(String email) {
        if (email == null || email.isBlank()) return java.util.Optional.empty();
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) return java.util.Optional.empty(); // не раскрываем, есть ли такой email
        passwordResetTokenRepository.deleteByUserUserId(user.getUserId());
        String token = generateToken();
        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setToken(token);
        prt.setExpiresAt(LocalDateTime.now().plusHours(RESET_TOKEN_VALIDITY_HOURS));
        prt.setCreatedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(prt);
        return java.util.Optional.of(token);
    }

    @Override
    @Transactional
    public Optional<String> resetPasswordByToken(String token, String newPassword) {
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("Не указан токен или новый пароль");
        }
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token.trim())
                .orElseThrow(() -> new RuntimeException("Ссылка для сброса пароля недействительна или уже использована"));
        if (prt.isExpired()) {
            passwordResetTokenRepository.delete(prt);
            throw new RuntimeException("Срок действия ссылки истёк. Запросите сброс пароля снова.");
        }
        User user = prt.getUser();
        String email = user.getEmail();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.delete(prt);
        return Optional.ofNullable(email).filter(e -> !e.isBlank());
    }

    @Override
    public Optional<String> findEmailByLogin(String login) {
        if (login == null || login.isBlank()) return Optional.empty();
        return userRepository.findByLogin(login.trim()).map(User::getEmail).filter(e -> e != null && !e.isBlank());
    }

    /** Генерирует токен для сброса (48 байт -> 64 символа base64). */
    private static String generateToken() {
        byte[] bytes = new byte[48];
        RND.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
