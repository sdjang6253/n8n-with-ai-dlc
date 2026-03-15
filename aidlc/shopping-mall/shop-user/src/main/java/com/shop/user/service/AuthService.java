package com.shop.user.service;

import com.shop.user.dto.AuthResponse;
import com.shop.user.dto.LoginRequest;
import com.shop.user.dto.LoginResponse;
import com.shop.user.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AuthService delegates to UserService.
 * Kept for backward compatibility with existing tests.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    public void register(RegisterRequest request) {
        userService.register(request);
    }

    public LoginResponse login(LoginRequest request) {
        AuthResponse authResponse = userService.login(request);
        return new LoginResponse(authResponse.getAccessToken());
    }
}
