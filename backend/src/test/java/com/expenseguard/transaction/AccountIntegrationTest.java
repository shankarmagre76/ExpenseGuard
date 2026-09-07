package com.expenseguard.transaction;

import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.auth.service.AuthService;
import com.expenseguard.transaction.dto.AccountRequest;
import com.expenseguard.transaction.entity.Account;
import com.expenseguard.transaction.entity.AccountType;
import com.expenseguard.transaction.repository.AccountRepository;
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test suite verifying Phase 3.1 Accounts Management requirements.
 */
import com.expenseguard.ExpenseGuardApplication;

@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountIntegrationTest {

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
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private Account userAAccount;

    @BeforeEach
    void setUp() {
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

        // 3. Create default account for User A
        userAAccount = accountRepository.save(Account.builder()
                .user(userA)
                .name("User A Primary Account")
                .type(AccountType.BANK)
                .balance(new BigDecimal("15000.0000"))
                .currency("INR")
                .build());
    }

    @Test
    @DisplayName("TEST 1: User A creates account (201 Created, persisted in DB)")
    void test1_CreateAccount_UserA_Returns201Created() throws Exception {
        AccountRequest request = AccountRequest.builder()
                .name("HDFC Bank")
                .type(AccountType.BANK)
                .openingBalance(new BigDecimal("25000"))
                .currency("INR")
                .build();

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", is(userA.getId().toString())))
                .andExpect(jsonPath("$.name", is("HDFC Bank")))
                .andExpect(jsonPath("$.type", is("BANK")))
                .andExpect(jsonPath("$.balance", is(25000)));

        // Verify persistence
        assertThat(accountRepository.findByUserId(userA.getId())).hasSize(2);
    }

    @Test
    @DisplayName("TEST 2: Get my accounts (User A receives only User A accounts)")
    void test2_GetMyAccounts_UserA_ReturnsUserAAccountsOnly() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("User A Primary Account")));
    }

    @Test
    @DisplayName("TEST 3: Get account by ID (User A receives account data)")
    void test3_GetAccountById_UserA_Returns200OK() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/" + userAAccount.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userAAccount.getId().toString())))
                .andExpect(jsonPath("$.name", is("User A Primary Account")));
    }

    @Test
    @DisplayName("TEST 4: User B cannot access User A's account (403 Forbidden)")
    void test4_UserB_CannotAccessUserAAccount_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/" + userAAccount.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("TEST 5: User B creates own account (201 Created, belongs to User B)")
    void test5_UserB_CreatesOwnAccount_Returns201Created() throws Exception {
        AccountRequest request = AccountRequest.builder()
                .name("Cash")
                .type(AccountType.CASH)
                .openingBalance(new BigDecimal("5000"))
                .currency("INR")
                .build();

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(userB.getId().toString())))
                .andExpect(jsonPath("$.name", is("Cash")))
                .andExpect(jsonPath("$.type", is("CASH")));

        assertThat(accountRepository.findByUserId(userB.getId())).hasSize(1);
    }

    @Test
    @DisplayName("TEST 6: User-specific list (User B list contains zero User A accounts)")
    void test6_UserSpecificList_UserB_DoesNotContainUserAAccounts() throws Exception {
        // Create an account for User B
        accountRepository.save(Account.builder()
                .user(userB)
                .name("User B Wallet")
                .type(AccountType.WALLET)
                .balance(new BigDecimal("300.0000"))
                .currency("INR")
                .build());

        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("User B Wallet")));
    }

    @Test
    @DisplayName("TEST 7: Update own account (User A updates account successfully)")
    void test7_UpdateOwnAccount_UserA_Returns200OK() throws Exception {
        AccountRequest updateRequest = AccountRequest.builder()
                .name("HDFC Savings")
                .type(AccountType.SAVINGS)
                .openingBalance(new BigDecimal("30000"))
                .currency("INR")
                .build();

        mockMvc.perform(put("/api/v1/accounts/" + userAAccount.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("HDFC Savings")))
                .andExpect(jsonPath("$.type", is("SAVINGS")))
                .andExpect(jsonPath("$.balance", is(30000)));

        Optional<Account> updatedInDb = accountRepository.findById(userAAccount.getId());
        assertThat(updatedInDb).isPresent();
        assertThat(updatedInDb.get().getName()).isEqualTo("HDFC Savings");
    }

    @Test
    @DisplayName("TEST 8: Update other user's account (User B update on User A account returns 403)")
    void test8_UpdateOtherUserAccount_Returns403Forbidden() throws Exception {
        AccountRequest updateRequest = AccountRequest.builder()
                .name("Hacked Account")
                .type(AccountType.SAVINGS)
                .openingBalance(new BigDecimal("99999"))
                .currency("INR")
                .build();

        mockMvc.perform(put("/api/v1/accounts/" + userAAccount.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        // Account in DB remains intact
        Account accountInDb = accountRepository.findById(userAAccount.getId()).orElseThrow();
        assertThat(accountInDb.getName()).isEqualTo("User A Primary Account");
    }

    @Test
    @DisplayName("TEST 9: Delete own account (User A deletes account successfully)")
    void test9_DeleteOwnAccount_UserA_Returns204NoContent() throws Exception {
        Account tempAccount = accountRepository.save(Account.builder()
                .user(userA)
                .name("Temporary Account")
                .type(AccountType.CASH)
                .balance(new BigDecimal("100.0000"))
                .currency("INR")
                .build());

        mockMvc.perform(delete("/api/v1/accounts/" + tempAccount.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        assertThat(accountRepository.findById(tempAccount.getId())).isEmpty();
    }

    @Test
    @DisplayName("TEST 10: Delete other user's account (User B delete on User A account returns 403)")
    void test10_DeleteOtherUserAccount_Returns403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/accounts/" + userAAccount.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        assertThat(accountRepository.findById(userAAccount.getId())).isPresent();
    }

    @Test
    @DisplayName("TEST 11: No JWT (Unauthenticated request returns 401 Unauthorized)")
    void test11_NoJwt_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TEST 12: Invalid data (Validation failure returns 400 Bad Request)")
    void test12_InvalidData_Returns400BadRequest() throws Exception {
        String invalidJson = """
                {
                  "name": "",
                  "type": null,
                  "openingBalance": null
                }
                """;

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("TEST 13: Client-supplied user ID in payload is ignored; JWT identity is enforced")
    void test13_ClientSuppliedUserId_IsIgnoredByServer() throws Exception {
        String spoofedPayload = String.format("""
                {
                  "name": "Spoofed Account",
                  "type": "BANK",
                  "openingBalance": 1000,
                  "userId": "%s"
                }
                """, userB.getId());

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spoofedPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(userA.getId().toString())));
    }
}
