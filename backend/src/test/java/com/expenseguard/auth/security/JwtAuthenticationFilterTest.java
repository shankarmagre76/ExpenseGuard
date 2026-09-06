package com.expenseguard.auth.security;

import com.expenseguard.auth.dto.LoginRequest;
import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

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

    private User testUserA;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Register default test user A
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("User A")
                .email("usera@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(registerRequest);

        Optional<User> userOptional = userRepository.findByEmail("usera@example.com");
        assertThat(userOptional).isPresent();
        testUserA = userOptional.get();
    }

    @Test
    @DisplayName("TEST 1: Health endpoint is accessible without token")
    void health_WithoutToken_Returns200OK() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("TEST 2: Login issues valid access token")
    void login_ReturnsValidToken() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("usera@example.com")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")));
    }

    @Test
    @DisplayName("TEST 3: Protected endpoint without token returns 401 Unauthorized")
    void protectedEndpoint_WithoutToken_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("TEST 4: Protected endpoint with valid token returns 200 OK and user info")
    void protectedEndpoint_WithValidToken_Returns200OK() throws Exception {
        String token = jwtService.generateToken(testUserA);

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(testUserA.getId().toString())))
                .andExpect(jsonPath("$.email", is("usera@example.com")))
                .andExpect(jsonPath("$.name", is("User A")));
    }

    @Test
    @DisplayName("TEST 5: Malformed token returns 401 Unauthorized")
    void protectedEndpoint_WithMalformedToken_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("TEST 6: Tampered token returns 401 Unauthorized")
    void protectedEndpoint_WithTamperedToken_Returns401Unauthorized() throws Exception {
        String token = jwtService.generateToken(testUserA);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("TEST 7: Expired token returns 401 Unauthorized")
    void protectedEndpoint_WithExpiredToken_Returns401Unauthorized() throws Exception {
        // Generate an already expired token (-10 seconds)
        String expiredToken = jwtService.generateToken(testUserA, -10000);

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("TEST 8: Invalid Authorization scheme (Basic) returns 401 Unauthorized")
    void protectedEndpoint_WithBasicAuthScheme_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Basic abc123xyz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("TEST 9: Public register endpoint accessible without token")
    void register_WithoutToken_IsAccessible() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("User B")
                .email("userb@example.com")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("userb@example.com")));
    }

    @Test
    @DisplayName("TEST 10: Public login endpoint accessible without token")
    void login_WithoutToken_IsAccessible() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("usera@example.com")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));
    }

    @Test
    @DisplayName("TEST 11: Security test - Valid JWT for User B identifies User B, not User A")
    void protectedEndpoint_IdentifiesCorrectUserFromJwt() throws Exception {
        // Register User B
        RegisterRequest registerB = RegisterRequest.builder()
                .name("User B")
                .email("userb@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(registerB);
        User userB = userRepository.findByEmail("userb@example.com").orElseThrow();

        // Generate token for User B
        String tokenB = jwtService.generateToken(userB);

        // Call protected endpoint with User B token
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userB.getId().toString())))
                .andExpect(jsonPath("$.email", is("userb@example.com")))
                .andExpect(jsonPath("$.name", is("User B")));
    }
}
