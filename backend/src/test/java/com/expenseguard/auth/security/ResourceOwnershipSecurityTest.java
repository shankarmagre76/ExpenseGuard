package com.expenseguard.auth.security;

import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.service.AuthService;
import com.expenseguard.transaction.entity.Account;
import com.expenseguard.transaction.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test suite verifying Phase 2.5 Authorization and Resource Ownership Security requirements.
 */
import com.expenseguard.ExpenseGuardApplication;

@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceOwnershipSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private com.expenseguard.budget.repository.BudgetRepository budgetRepository;

    @Autowired
    private com.expenseguard.transaction.repository.TransactionRepository transactionRepository;

    @Autowired
    private com.expenseguard.transaction.repository.CategoryRepository categoryRepository;

    private User userA;
    private User userB;
    private Account accountA;
    private Account accountB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register User A
        RegisterRequest registerA = RegisterRequest.builder()
                .name("User A")
                .email("testa@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(registerA);
        userA = userRepository.findByEmail("testa@example.com").orElseThrow();
        tokenA = jwtService.generateToken(userA);

        // 2. Register User B
        RegisterRequest registerB = RegisterRequest.builder()
                .name("User B")
                .email("testb@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(registerB);
        userB = userRepository.findByEmail("testb@example.com").orElseThrow();
        tokenB = jwtService.generateToken(userB);

        // 3. Create Account for User A
        accountA = accountRepository.save(Account.builder()
                .user(userA)
                .name("User A Checking")
                .currency("USD")
                .balance(new BigDecimal("1000.0000"))
                .build());

        // 4. Create Account for User B
        accountB = accountRepository.save(Account.builder()
                .user(userB)
                .name("User B Savings")
                .currency("EUR")
                .balance(new BigDecimal("2500.0000"))
                .build());
    }

    @Test
    @DisplayName("TEST 1: User A uses User A JWT → GET /api/v1/me returns User A data")
    void test1_UserA_GetMe_ReturnsUserAData() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userA.getId().toString())))
                .andExpect(jsonPath("$.email", is("testa@example.com")))
                .andExpect(jsonPath("$.name", is("User A")));
    }

    @Test
    @DisplayName("TEST 2: User B uses User B JWT → GET /api/v1/me returns User B data")
    void test2_UserB_GetMe_ReturnsUserBData() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userB.getId().toString())))
                .andExpect(jsonPath("$.email", is("testb@example.com")))
                .andExpect(jsonPath("$.name", is("User B")));
    }

    @Test
    @DisplayName("TEST 3: Unauthenticated request to protected endpoint returns 401 Unauthorized")
    void test3_Unauthenticated_ProtectedEndpoint_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/" + accountA.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("TEST 4: User A requests User B's owned account → 403 Forbidden (IDOR Protection)")
    void test4_UserA_RequestsUserBAccount_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/" + accountB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access denied: You do not have permission to access this account")));
    }

    @Test
    @DisplayName("TEST 5: User B requests User B's own account → 200 OK")
    void test5_UserB_RequestsOwnAccount_Returns200OK() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/" + accountB.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(accountB.getId().toString())))
                .andExpect(jsonPath("$.userId", is(userB.getId().toString())))
                .andExpect(jsonPath("$.name", is("User B Savings")))
                .andExpect(jsonPath("$.currency", is("EUR")));
    }

    @Test
    @DisplayName("TEST 6: Attempting to pass client userId parameter is ignored; server enforces JWT identity")
    void test6_ClientPassesDifferentUserId_ServerEnforcesJwtIdentity() throws Exception {
        // User A requests User A's account while passing User B's userId in query parameter
        mockMvc.perform(get("/api/v1/accounts/" + accountA.getId())
                        .param("userId", userB.getId().toString())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(accountA.getId().toString())))
                .andExpect(jsonPath("$.userId", is(userA.getId().toString())))
                .andExpect(jsonPath("$.name", is("User A Checking")));
    }

    @Test
    @DisplayName("TEST 7: Nonexistent resource ID lookup → 404 Not Found without leaking ownership")
    void test7_NonexistentResourceId_Returns404NotFound() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/accounts/" + randomId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }
}
