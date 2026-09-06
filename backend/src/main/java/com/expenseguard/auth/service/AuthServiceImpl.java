package com.expenseguard.auth.service;

import com.expenseguard.auth.dto.LoginRequest;
import com.expenseguard.auth.dto.LoginResponse;
import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.dto.RegisterResponse;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.entity.UserStatus;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.common.exception.EmailAlreadyExistsException;
import com.expenseguard.common.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for user authentication, login, and account registration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public RegisterResponse registerUser(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Email Uniqueness Check (Case-Insensitive)
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException("A user with email '" + normalizedEmail + "' already exists");
        }

        // 2. Secure Password Hashing using BCrypt
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 3. User Entity Creation
        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .passwordHash(hashedPassword)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {} and email: {}", savedUser.getId(), savedUser.getEmail());

        // 4. Return Safe Response DTO (excluding password hash)
        return RegisterResponse.builder()
                .message("User registered successfully")
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse loginUser(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Fetch user by email (case-insensitive)
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // 2. Verify password using PasswordEncoder (BCrypt)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // 3. Generate JWT access token
        String accessToken = jwtService.generateToken(user);
        log.info("User logged in successfully with ID: {}", user.getId());

        // 4. Return safe LoginResponse payload
        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpirationMs())
                .build();
    }
}
