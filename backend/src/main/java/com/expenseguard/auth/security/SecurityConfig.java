package com.expenseguard.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Main Security Configuration for ExpenseGuard Backend.
 * <p>
 * Establishes stateless session management for REST APIs, configures endpoint authorization rules,
 * disables CSRF for stateless API access, and provides the PasswordEncoder bean.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the Spring Security FilterChain.
     *
     * @param http HttpSecurity configuration builder
     * @return Built SecurityFilterChain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF since REST APIs use stateless authentication (e.g., JWT)
                .csrf(AbstractHttpConfigurer::disable)
                // Enforce stateless session management (no HTTP sessions stored on server)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configure request level permissions
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints accessible without authentication
                        .requestMatchers("/api/v1/health", "/api/v1/auth/**").permitAll()
                        // All other API endpoints require authentication
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /**
     * Configures the password encoder bean using BCrypt hashing algorithm.
     * BCrypt is a secure, adaptive password hashing function that includes key strengthening (salting + work factor).
     *
     * @return PasswordEncoder instance using BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
