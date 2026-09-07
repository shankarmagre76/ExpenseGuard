package com.expenseguard.common;

import com.expenseguard.ExpenseGuardApplication;
import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.auth.service.AuthService;
import com.expenseguard.budget.repository.BudgetRepository;
import com.expenseguard.transaction.entity.Account;
import com.expenseguard.transaction.entity.AccountType;
import com.expenseguard.transaction.entity.Category;
import com.expenseguard.transaction.entity.CategoryType;
import com.expenseguard.transaction.entity.Transaction;
import com.expenseguard.transaction.entity.TransactionType;
import com.expenseguard.transaction.repository.AccountRepository;
import com.expenseguard.transaction.repository.CategoryRepository;
import com.expenseguard.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 6.1 Hardening Test Suite: API Pagination Boundaries and Page Size Capping.
 */
@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaginationHardeningTest {

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

    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        RegisterRequest register = RegisterRequest.builder()
                .name("Page User")
                .email("pageuser@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(register);
        user = userRepository.findByEmail("pageuser@example.com").orElseThrow();
        token = "Bearer " + jwtService.generateToken(user);

        Account account = accountRepository.save(Account.builder()
                .user(user)
                .name("Page Account")
                .type(AccountType.BANK)
                .balance(new BigDecimal("1000.00"))
                .build());

        Category category = categoryRepository.save(Category.builder()
                .user(user)
                .name("Page Category")
                .type(CategoryType.EXPENSE)
                .build());

        for (int i = 0; i < 5; i++) {
            transactionRepository.save(Transaction.builder()
                    .user(user)
                    .account(account)
                    .category(category)
                    .type(TransactionType.EXPENSE)
                    .amount(new BigDecimal("10.00"))
                    .transactionDate(LocalDate.now())
                    .build());
        }
    }

    @Test
    @DisplayName("Pagination 1: page=0 and size=2 returns page size 2")
    void pageZeroSizeTwo() throws Exception {
        mockMvc.perform(get("/api/v1/transactions?page=0&size=2")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(5)));
    }

    @Test
    @DisplayName("Pagination 2: Excessive page size (150) is capped at server max 100")
    void excessiveSizeCappedAt100() throws Exception {
        mockMvc.perform(get("/api/v1/transactions?page=0&size=150")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(100)));
    }
}
