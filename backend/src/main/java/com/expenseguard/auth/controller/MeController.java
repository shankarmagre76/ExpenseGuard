package com.expenseguard.auth.controller;

import com.expenseguard.auth.dto.UserResponse;
import com.expenseguard.auth.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller mapping /api/v1/me endpoint for current user profile retrieval.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    /**
     * Protected endpoint returning current authenticated user information.
     *
     * @param currentUser Authenticated user resolved from SecurityContext
     * @return 200 OK status and user profile payload
     */
    @GetMapping
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal User currentUser) {
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
