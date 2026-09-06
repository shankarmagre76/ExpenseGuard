package com.expenseguard.auth.security;

import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.entity.UserStatus;
import com.expenseguard.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Security Filter that intercepts incoming HTTP requests to validate JWT Bearer tokens
 * and establish the SecurityContext authentication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);

            if (jwt != null && jwtService.validateToken(jwt)) {
                String userIdStr = jwtService.getUserIdFromToken(jwt);

                if (userIdStr != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UUID userId = UUID.fromString(userIdStr);
                    Optional<User> userOptional = userRepository.findById(userId);

                    if (userOptional.isPresent()) {
                        User user = userOptional.get();

                        if (user.getStatus() == UserStatus.ACTIVE) {
                            org.springframework.security.core.authority.SimpleGrantedAuthority authority =
                                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER");
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(user, null, java.util.List.of(authority));
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("Successfully authenticated User ID: {}", userId);
                        } else {
                            log.warn("User ID {} status is not ACTIVE ({})", userId, user.getStatus());
                        }
                    } else {
                        log.warn("User ID {} from valid JWT not found in database", userId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not set user authentication in SecurityContext: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts Bearer token from the HTTP Authorization header.
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
