package ru.ryabov.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ryabov.dto.AuthRequest;
import ru.ryabov.dto.AuthResponse;
import ru.ryabov.dto.RegisterRequest;
import ru.ryabov.dto.UpdateProfileRequest;
import ru.ryabov.dto.UserDto;
import ru.ryabov.model.User;
import ru.ryabov.repository.UserRepository;
import ru.ryabov.security.Roles;
import ru.ryabov.security.JwtService;
import ru.ryabov.service.AuthService;
import ru.ryabov.util.UrlHelper;

import java.time.OffsetDateTime;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Roles.USER)
                .dateJoined(OffsetDateTime.now())
                .build();
        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);
        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(null)
                .expiresIn(jwtService.getExpirationMs())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new BadCredentialsException("Неверный логин или пароль"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Неверный логин или пароль");
        }
        user.setLastLogin(OffsetDateTime.now());
        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(null)
                .expiresIn(jwtService.getExpirationMs())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return toDto(user);
    }

    @Override
    @Transactional
    public UserDto updateCurrentUser(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("Пользователь с таким именем уже существует");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Пользователь с таким email уже существует");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getDisplayName() != null) {
            String display = request.getDisplayName().trim();
            user.setDisplayName(display.isEmpty() ? null : display);
        }

        if (request.getAvatarUrl() != null) {
            String avatar = request.getAvatarUrl().trim();
            user.setAvatarUrl(avatar.isEmpty() ? null : avatar);
        }

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                UrlHelper.toAbsolute(user.getAvatarUrl()),
                user.getSubscribers().size(),
                user.getSubscriptions().size()
        );
    }
}
