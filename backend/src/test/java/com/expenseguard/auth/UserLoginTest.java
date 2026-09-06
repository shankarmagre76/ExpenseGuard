package com.expenseguard.auth;

import com.expenseguard.auth.dto.LoginRequest;
import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${expenseguard.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Valid login returns 200 OK with Bearer token and valid JWT claims")
    void login_ValidCredentials_Returns200OKWithJwtToken() throws Exception {
        // Register test user first
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Login User")
                .email("login@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(registerRequest);

        Optional<User> userOptional = userRepository.findByEmail("login@example.com");
        assertThat(userOptional).isPresent();
        User registeredUser = userOptional.get();

        // Perform login with case-insensitive email
        LoginRequest loginRequest = LoginRequest.builder()
                .email("LOGIN@EXAMPLE.COM")
                .password("Password@123")
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(86400000)))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Parse generated JWT token to verify claims
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String token = jsonNode.get("accessToken").asText();

        SecretKey key;
        try {
            key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        } catch (Exception e) {
            key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        }

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(registeredUser.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("login@example.com");
        assertThat(claims.get("name", String.class)).isEqualTo("Login User");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("2. Non-existent email returns 401 Unauthorized with generic message")
    void login_NonExistentEmail_Returns401Unauthorized() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("3. Incorrect password returns 401 Unauthorized with generic message")
    void login_IncorrectPassword_Returns401Unauthorized() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Login User")
                .email("user@example.com")
                .password("CorrectPassword123")
                .build();
        authService.registerUser(registerRequest);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("user@example.com")
                .password("WrongPassword123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("4. Invalid email format returns 400 Bad Request")
    void login_InvalidEmailFormat_Returns400BadRequest() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("not-an-email")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.email", is("Invalid email format")));
    }

    @Test
    @DisplayName("5. Blank password returns 400 Bad Request")
    void login_BlankPassword_Returns400BadRequest() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("user@example.com")
                .password("")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.password", is("Password must not be blank")));
    }
}
