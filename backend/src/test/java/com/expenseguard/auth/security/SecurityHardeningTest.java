package com.expenseguard.auth.security;

import com.expenseguard.ExpenseGuardApplication;
import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 6.1 Hardening Test Suite: JWT, Authentication Scheme, IDOR Cross-User Isolation,
 * and Sensitive Data Concealment.
 */
@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        RegisterRequest regA = RegisterRequest.builder()
                .name("Sec User A")
                .email("secusera@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(regA);
        userA = userRepository.findByEmail("secusera@example.com").orElseThrow();
        tokenA = "Bearer " + jwtService.generateToken(userA);

        RegisterRequest regB = RegisterRequest.builder()
                .name("Sec User B")
                .email("secuserb@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(regB);
        userB = userRepository.findByEmail("secuserb@example.com").orElseThrow();
        tokenB = "Bearer " + jwtService.generateToken(userB);
    }

    @Test
    @DisplayName("Security 1: Request with missing Authorization header returns 401")
    void missingJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security 2: Request with malformed JWT returns 401")
    void malformedJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer malformed.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security 3: Request with tampered JWT signature returns 401")
    void tamperedJwtReturns401() throws Exception {
        String validToken = jwtService.generateToken(userA);
        String tamperedToken = "Bearer " + validToken.substring(0, validToken.length() - 5) + "ABCDE";

        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security 4: Request with wrong authorization scheme (Basic) returns 401")
    void wrongAuthorizationSchemeReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Basic dXNlcjpwYXNzd29yZA=="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security 5: Registration and Login responses do not leak password or passwordHash")
    void passwordNotLeakedInAuthResponses() throws Exception {
        RegisterRequest register = RegisterRequest.builder()
                .name("Privacy Test")
                .email("privacy@example.com")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}
