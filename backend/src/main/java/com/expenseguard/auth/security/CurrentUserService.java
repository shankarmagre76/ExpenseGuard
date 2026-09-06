package com.expenseguard.auth.security;

import com.expenseguard.auth.entity.User;

import java.util.UUID;

/**
 * Service contract for retrieving the currently authenticated user's identity from Spring SecurityContext.
 */
public interface CurrentUserService {

    /**
     * Retrieves the unique ID of the currently authenticated user.
     *
     * @return UUID of current user
     */
    UUID getCurrentUserId();

    /**
     * Retrieves the complete User entity of the currently authenticated user.
     *
     * @return User instance
     */
    User getCurrentUser();
}
