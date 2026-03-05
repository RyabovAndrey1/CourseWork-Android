package ru.ryabov.service;

import ru.ryabov.dto.AuthRequest;
import ru.ryabov.dto.AuthResponse;
import ru.ryabov.dto.RegisterRequest;
import ru.ryabov.dto.UpdateProfileRequest;
import ru.ryabov.dto.UserDto;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
    UserDto getCurrentUser(Long userId);
    UserDto updateCurrentUser(Long userId, UpdateProfileRequest request);
}
