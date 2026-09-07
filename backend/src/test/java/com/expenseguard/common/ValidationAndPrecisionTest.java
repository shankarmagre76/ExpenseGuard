package com.expenseguard.common;

import com.expenseguard.ExpenseGuardApplication;
import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.auth.service.AuthService;
import com.expenseguard.budget.repository.BudgetRepository;
import com.expenseguard.transaction.dto.TransactionRequest;
import com.expenseguard.transaction.entity.Account;
import com.expenseguard.transaction.entity.AccountType;
import com.expenseguard.transaction.entity.Category;
import com.expenseguard.transaction.entity.CategoryType;
import com.expenseguard.transaction.entity.TransactionType;
import com.expenseguard.transaction.repository.AccountRepository;
import com.expenseguard.transaction.repository.CategoryRepository;
import com.expenseguard.transaction.repository.TransactionRepository;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 6.1 Hardening Test Suite: Input validation boundaries and BigDecimal precision exactness.
 */
@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidationAndPrecisionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private String token;
    private Account account;
    private Category category;

    @BeforeEach
    void setUp() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        RegisterRequest register = RegisterRequest.builder()
                .name("Val User")
                .email("valuser@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(register);
        user = userRepository.findByEmail("valuser@example.com").orElseThrow();
        token = "Bearer " + jwtService.generateToken(user);

        account = accountRepository.save(Account.builder()
                .user(user)
                .name("Val Account")
                .type(AccountType.BANK)
                .balance(new BigDecimal("100.0000"))
                .build());

        category = categoryRepository.save(Category.builder()
                .user(user)
                .name("Val Category")
                .type(CategoryType.EXPENSE)
                .build());
    }

    @Test
    @DisplayName("Validation 1: Zero amount returns 400 Bad Request")
    void zeroAmountReturns400() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(account.getId())
                .categoryId(category.getId())
                .type(TransactionType.EXPENSE)
                .amount(BigDecimal.ZERO)
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation 2: Negative amount returns 400 Bad Request")
    void negativeAmountReturns400() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(account.getId())
                .categoryId(category.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("-15.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation 3: Very small positive amount (0.01) succeeds")
    void smallPositiveAmountSucceeds() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(account.getId())
                .categoryId(category.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("0.01"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Validation 4: Description exceeding 255 characters returns 400 Bad Request")
    void longDescriptionReturns400() throws Exception {
        String longDesc = "a".repeat(256);
        TransactionRequest request = TransactionRequest.builder()
                .accountId(account.getId())
                .categoryId(category.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("50.00"))
                .transactionDate(LocalDate.now())
                .description(longDesc)
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Precision 1: 100.00 - 0.10 - 0.20 equals exactly 99.7000 without floating point drift")
    void bigDecimalPrecisionExactness() throws Exception {
        // Initial balance 100.00
        // Expense 0.10
        TransactionRequest req1 = TransactionRequest.builder()
                .accountId(account.getId())
                .categoryId(category.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("0.10"))
                .transactionDate(LocalDate.now())
                .build();
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Expense 0.20
        TransactionRequest req2 = TransactionRequest.builder()
                .accountId(account.getId())
                .categoryId(category.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("0.20"))
                .transactionDate(LocalDate.now())
                .build();
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        Account updated = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo("99.7000");
    }
}
