package com.expenseguard.auth;

import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.expenseguard.budget.repository.BudgetRepository budgetRepository;

    @Autowired
    private com.expenseguard.transaction.repository.TransactionRepository transactionRepository;

    @Autowired
    private com.expenseguard.transaction.repository.CategoryRepository categoryRepository;

    @Autowired
    private com.expenseguard.transaction.repository.AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("A. Valid registration returns 201 Created and safe user details")
    void register_ValidPayload_Returns201Created() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .password("Test@12345")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("User registered successfully")))
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.name", is("Test User")))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("B. Duplicate email registration returns 409 Conflict (case-insensitive)")
    void register_DuplicateEmail_Returns409Conflict() throws Exception {
        RegisterRequest firstRequest = RegisterRequest.builder()
                .name("First User")
                .email("test@example.com")
                .password("Test@12345")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Duplicate registration attempt with uppercase email
        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .name("Second User")
                .email("TEST@EXAMPLE.COM")
                .password("DifferentPass123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("C. Invalid email format returns 400 Bad Request")
    void register_InvalidEmail_Returns400BadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .email("invalid-email-format")
                .password("Test@12345")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.email", is("Invalid email format")));
    }

    @Test
    @DisplayName("D. Short password returns 400 Bad Request")
    void register_ShortPassword_Returns400BadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .password("short")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.password", is("Password must be at least 8 characters long")));
    }

    @Test
    @DisplayName("E. Missing name returns 400 Bad Request")
    void register_MissingName_Returns400BadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("")
                .email("test@example.com")
                .password("Test@12345")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.name", is("Name must not be blank")));
    }

    @Test
    @DisplayName("F. Password security: Verifies BCrypt hash in DB and raw password non-storage")
    void register_PasswordSecurity_StoresHashedPasswordInDatabase() throws Exception {
        String rawPassword = "SecurePassword@123";
        RegisterRequest request = RegisterRequest.builder()
                .name("Security User")
                .email("security@example.com")
                .password(rawPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Optional<User> userOptional = userRepository.findByEmail("security@example.com");
        assertThat(userOptional).isPresent();

        User savedUser = userOptional.get();
        // 1. Password must NOT be stored as plaintext
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(rawPassword);
        // 2. Hash must start with BCrypt identifier ($2a$ or $2b$)
        assertThat(savedUser.getPasswordHash()).startsWith("$2a$");
        // 3. PasswordEncoder must match raw password against stored hash
        assertThat(passwordEncoder.matches(rawPassword, savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("G. Response security: Verifies sensitive fields are not in response payload")
    void register_ResponseSecurity_ExcludesCredentialsFromPayload() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Response User")
                .email("response@example.com")
                .password("Test@12345")
                .build();

        String content = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).doesNotContain("password");
        assertThat(content).doesNotContain("passwordHash");
        assertThat(content).doesNotContain("Test@12345");
    }
}
