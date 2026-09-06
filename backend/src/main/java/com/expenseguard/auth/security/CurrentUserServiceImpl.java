package com.expenseguard.auth.security;

import com.expenseguard.auth.entity.User;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Spring Service implementing CurrentUserService to safely extract authenticated user details
 * directly from SecurityContextHolder.
 */
@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    @Override
    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            throw new InsufficientAuthenticationException("No authenticated user found in SecurityContext");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        throw new InsufficientAuthenticationException("Principal in SecurityContext is not a valid User");
    }
}
