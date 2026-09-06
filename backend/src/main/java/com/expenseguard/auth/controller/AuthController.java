package com.expenseguard.auth.controller;

import com.expenseguard.auth.dto.LoginRequest;
import com.expenseguard.auth.dto.LoginResponse;
import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.dto.RegisterResponse;
import com.expenseguard.auth.dto.UserResponse;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing user authentication, login, registration, and profile endpoints under /api/v1/auth.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user account.
     *
     * @param request Registration payload containing name, email, and password
     * @return 201 Created status and safe user registration response payload
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates user credentials and issues a JWT access token.
     *
     * @param request Login payload containing email and password
     * @return 200 OK status and safe login response payload containing JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.loginUser(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Protected endpoint returning current authenticated user profile.
     * Requires valid JWT Bearer token in Authorization header.
     *
     * @param currentUser Authenticated user resolved from SecurityContext
     * @return 200 OK status and user profile payload
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        UserResponse response = UserResponse.builder()
                .userId(currentUser.getId())
                .name(currentUser.getName())
                .email(currentUser.getEmail())
                .status(currentUser.getStatus().name())
                .createdAt(currentUser.getCreatedAt())
                .build();
        return ResponseEntity.ok(response);
    }
}
