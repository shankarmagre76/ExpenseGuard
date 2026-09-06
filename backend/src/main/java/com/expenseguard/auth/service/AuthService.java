package com.expenseguard.auth.service;

import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.dto.RegisterResponse;

/**
 * Service interface handling authentication operations.
 */
public interface AuthService {

    /**
     * Registers a new user in the system with password hashing and uniqueness validation.
     *
     * @param request Registration details
     * @return Safe registration response containing public user info
     */
    RegisterResponse registerUser(RegisterRequest request);

    /**
     * Authenticates user credentials and generates a JWT access token.
     *
     * @param request Login credentials (email & password)
     * @return Safe login response containing JWT access token and token metadata
     */
    com.expenseguard.auth.dto.LoginResponse loginUser(com.expenseguard.auth.dto.LoginRequest request);
}
