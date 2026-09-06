package com.expenseguard.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for user registration response.
 * Excludes sensitive fields like password or credentials.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {

    private String message;
    private UUID userId;
    private String name;
    private String email;
    private Instant createdAt;
}
