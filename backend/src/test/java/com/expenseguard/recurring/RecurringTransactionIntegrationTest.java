package com.expenseguard.recurring;

import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.auth.service.AuthService;
import com.expenseguard.budget.repository.BudgetRepository;
import com.expenseguard.recurring.dto.RecurringTransactionRequest;
import com.expenseguard.recurring.dto.RecurringTransactionStatusRequest;
import com.expenseguard.recurring.entity.RecurrenceFrequency;
import com.expenseguard.recurring.entity.RecurringTransaction;
import com.expenseguard.recurring.repository.RecurringTransactionRepository;
import com.expenseguard.recurring.service.RecurringTransactionExecutionService;
import com.expenseguard.transaction.entity.Account;
import com.expenseguard.transaction.entity.AccountType;
import com.expenseguard.transaction.entity.Category;
import com.expenseguard.transaction.entity.CategoryType;
import com.expenseguard.transaction.entity.Transaction;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test suite verifying Phase 5.1 Recurring Transactions requirements.
 * Covers 35 test scenarios including CRUD, validation, authorization, recurrence rules, idempotency, and execution.
 */
import com.expenseguard.ExpenseGuardApplication;

@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecurringTransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private RecurringTransactionExecutionService executionService;

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
    private Account accountA;
    private Category categoryFoodA;
    private Category categorySalaryA;
    private Category categoryShoppingB;
    private Account accountB;

    @BeforeEach
    void setUp() {
        recurringTransactionRepository.deleteAll();
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register User A
        RegisterRequest regA = RegisterRequest.builder()
                .name("User A")
                .email("usera@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(regA);
        userA = userRepository.findByEmail("usera@example.com").orElseThrow();
        tokenA = jwtService.generateToken(userA);

        // 2. Register User B
        RegisterRequest regB = RegisterRequest.builder()
                .name("User B")
                .email("userb@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(regB);
        userB = userRepository.findByEmail("userb@example.com").orElseThrow();
        tokenB = jwtService.generateToken(userB);

        // 3. Accounts & Categories for User A
        accountA = accountRepository.save(Account.builder()
                .user(userA)
                .name("Primary Checking")
                .type(AccountType.BANK)
                .balance(new BigDecimal("100000.0000"))
                .currency("INR")
                .build());

        categoryFoodA = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Food")
                .type(CategoryType.EXPENSE)
                .build());

        categorySalaryA = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Salary")
                .type(CategoryType.INCOME)
                .build());

        // 4. Accounts & Categories for User B
        accountB = accountRepository.save(Account.builder()
                .user(userB)
                .name("User B Cash")
                .type(AccountType.CASH)
                .balance(new BigDecimal("5000.0000"))
                .currency("INR")
                .build());

        categoryShoppingB = categoryRepository.save(Category.builder()
                .user(userB)
                .name("Shopping")
                .type(CategoryType.EXPENSE)
                .build());
    }

    // --- CRUD API Tests ---

    @Test
    @DisplayName("TEST 1: Create monthly recurring expense (201 Created)")
    void test1_CreateMonthlyRecurringExpense() throws Exception {
        RecurringTransactionRequest req = RecurringTransactionRequest.builder()
                .accountId(accountA.getId())
                .categoryId(categoryFoodA.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("15000.00"))
                .description("Monthly Rent")
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1))
                .nextRunDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2027, 8, 31))
                .build();

        mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.accountId", is(accountA.getId().toString())))
                .andExpect(jsonPath("$.categoryId", is(categoryFoodA.getId().toString())))
                .andExpect(jsonPath("$.type", is("EXPENSE")))
                .andExpect(jsonPath("$.amount", is(15000.00)))
                .andExpect(jsonPath("$.frequency", is("MONTHLY")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("TEST 2: Create monthly recurring income (201 Created)")
    void test2_CreateMonthlyRecurringIncome() throws Exception {
        RecurringTransactionRequest req = RecurringTransactionRequest.builder()
                .accountId(accountA.getId())
                .categoryId(categorySalaryA.getId())
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("50000.00"))
                .description("Salary")
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1))
                .nextRunDate(LocalDate.of(2026, 9, 1))
                .build();

        mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("INCOME")))
                .andExpect(jsonPath("$.amount", is(50000.00)));
    }

    @Test
    @DisplayName("TEST 3: List own recurring transactions (200 OK)")
    void test3_ListOwnRecurringTransactions() throws Exception {
        recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.WEEKLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .build());

        mockMvc.perform(get("/api/v1/recurring-transactions")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].frequency", is("WEEKLY")));
    }

    @Test
    @DisplayName("TEST 4: Get own recurring transaction by ID (200 OK)")
    void test4_GetOwnRecurringTransactionById() throws Exception {
        RecurringTransaction saved = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.WEEKLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .build());

        mockMvc.perform(get("/api/v1/recurring-transactions/" + saved.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().toString())));
    }

    @Test
    @DisplayName("TEST 5: Update own recurring transaction (200 OK)")
    void test5_UpdateOwnRecurringTransaction() throws Exception {
        RecurringTransaction saved = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.WEEKLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .build());

        RecurringTransactionRequest updateReq = RecurringTransactionRequest.builder()
                .accountId(accountA.getId())
                .categoryId(categoryFoodA.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("2500.00"))
                .frequency(RecurrenceFrequency.WEEKLY)
                .startDate(LocalDate.of(2026, 9, 1))
                .nextRunDate(LocalDate.of(2026, 9, 1))
                .build();

        mockMvc.perform(put("/api/v1/recurring-transactions/" + saved.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(2500.00)));
    }

    @Test
    @DisplayName("TEST 6: Delete own recurring transaction (204 No Content)")
    void test6_DeleteOwnRecurringTransaction() throws Exception {
        RecurringTransaction saved = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.WEEKLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .build());

        mockMvc.perform(delete("/api/v1/recurring-transactions/" + saved.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        assertThat(recurringTransactionRepository.findById(saved.getId())).isEmpty();
    }

    // --- Ownership & Security Tests ---

    @Test
    @DisplayName("TEST 7: Cross-user GET returns 403 Forbidden")
    void test7_CrossUserGet_Returns403() throws Exception {
        RecurringTransaction saved = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.WEEKLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .build());

        mockMvc.perform(get("/api/v1/recurring-transactions/" + saved.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 8: Cross-user UPDATE returns 403 Forbidden")
    void test8_CrossUserUpdate_Returns403() throws Exception {
        RecurringTransaction saved = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.WEEKLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .build());

        RecurringTransactionRequest updateReq = RecurringTransactionRequest.builder()
                .accountId(accountB.getId())
                .categoryId(categoryShoppingB.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("3000.00"))
                .frequency(RecurrenceFrequency.WEEKLY)
                .startDate(LocalDate.of(2026, 9, 1))
                .nextRunDate(LocalDate.of(2026, 9, 1))
                .build();

        mockMvc.perform(put("/api/v1/recurring-transactions/" + saved.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 9: Cross-user DELETE returns 403 Forbidden")
    void test9_CrossUserDelete_Returns403() throws Exception {
        RecurringTransaction saved = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.WEEKLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .build());

        mockMvc.perform(delete("/api/v1/recurring-transactions/" + saved.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 10: Referencing another user's account returns 403 Forbidden")
    void test10_CrossUserAccount_Returns403() throws Exception {
        RecurringTransactionRequest req = RecurringTransactionRequest.builder()
                .accountId(accountB.getId()) // User B account
                .categoryId(categoryFoodA.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("1000.00"))
                .frequency(RecurrenceFrequency.DAILY)
                .startDate(LocalDate.of(2026, 9, 1))
                .nextRunDate(LocalDate.of(2026, 9, 1))
                .build();

        mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 11: Referencing another user's category returns 403 Forbidden")
    void test11_CrossUserCategory_Returns403() throws Exception {
        RecurringTransactionRequest req = RecurringTransactionRequest.builder()
                .accountId(accountA.getId())
                .categoryId(categoryShoppingB.getId()) // User B category
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("1000.00"))
                .frequency(RecurrenceFrequency.DAILY)
                .startDate(LocalDate.of(2026, 9, 1))
                .nextRunDate(LocalDate.of(2026, 9, 1))
                .build();

        mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 12: Missing JWT returns 401 Unauthorized")
    void test12_NoJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/recurring-transactions"))
                .andExpect(status().isUnauthorized());
    }

    // --- Validation Tests ---

    @Test
    @DisplayName("TEST 13: Invalid amount (<= 0) returns 400 Bad Request")
    void test13_InvalidAmount_Returns400() throws Exception {
        RecurringTransactionRequest req = RecurringTransactionRequest.builder()
                .accountId(accountA.getId())
                .categoryId(categoryFoodA.getId())
                .type(TransactionType.EXPENSE)
                .amount(BigDecimal.ZERO)
                .frequency(RecurrenceFrequency.DAILY)
                .startDate(LocalDate.of(2026, 9, 1))
                .nextRunDate(LocalDate.of(2026, 9, 1))
                .build();

        mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 14: Missing account returns 400 Bad Request")
    void test14_MissingAccount_Returns400() throws Exception {
        RecurringTransactionRequest req = RecurringTransactionRequest.builder()
                .accountId(null)
                .categoryId(categoryFoodA.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .frequency(RecurrenceFrequency.DAILY)
                .startDate(LocalDate.of(2026, 9, 1))
                .nextRunDate(LocalDate.of(2026, 9, 1))
                .build();

        mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 15: Missing category returns 400 Bad Request")
    void test15_MissingCategory_Returns400() throws Exception {
        RecurringTransactionRequest req = RecurringTransactionRequest.builder()
                .accountId(accountA.getId())
                .categoryId(null)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .frequency(RecurrenceFrequency.DAILY)
                .startDate(LocalDate.of(2026, 9, 1))
                .nextRunDate(LocalDate.of(2026, 9, 1))
                .build();

        mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 16: Invalid frequency returns 400 Bad Request")
    void test16_InvalidFrequency_Returns400() throws Exception {
        String json = String.format("""
                {
                  "accountId": "%s",
                  "categoryId": "%s",
                  "type": "EXPENSE",
                  "amount": 500,
                  "frequency": "INVALID_FREQ",
                  "startDate": "2026-09-01",
                  "nextRunDate": "2026-09-01"
                }
                """, accountA.getId(), categoryFoodA.getId());

        mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 17: Invalid date relationship (nextRunDate < startDate) returns 400 Bad Request")
    void test17_InvalidDateRelationship_Returns400() throws Exception {
        RecurringTransactionRequest req = RecurringTransactionRequest.builder()
                .accountId(accountA.getId())
                .categoryId(categoryFoodA.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .frequency(RecurrenceFrequency.DAILY)
                .startDate(LocalDate.of(2026, 9, 10))
                .nextRunDate(LocalDate.of(2026, 9, 1)) // before startDate
                .build();

        mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 18: Category/type mismatch (EXPENSE with INCOME category) returns 400 Bad Request")
    void test18_CategoryTypeMismatch_Returns400() throws Exception {
        RecurringTransactionRequest req = RecurringTransactionRequest.builder()
                .accountId(accountA.getId())
                .categoryId(categorySalaryA.getId()) // INCOME category
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .frequency(RecurrenceFrequency.DAILY)
                .startDate(LocalDate.of(2026, 9, 1))
                .nextRunDate(LocalDate.of(2026, 9, 1))
                .build();

        mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- Status Toggle & Execution Tests ---

    @Test
    @DisplayName("TEST 19: Disable recurring transaction (PUT /status)")
    void test19_DisableRecurringTransaction() throws Exception {
        RecurringTransaction saved = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        RecurringTransactionStatusRequest statusReq = RecurringTransactionStatusRequest.builder()
                .active(false)
                .build();

        mockMvc.perform(put("/api/v1/recurring-transactions/" + saved.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    @DisplayName("TEST 20: Disabled transaction does not execute")
    void test20_DisabledTransaction_DoesNotExecute() {
        recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(false) // Disabled
                .build());

        int count = executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 5));
        assertThat(count).isEqualTo(0);
        assertThat(transactionRepository.findAllByUserId(userA.getId())).isEmpty();
    }

    @Test
    @DisplayName("TEST 21: Due transaction executes")
    void test21_DueTransaction_Executes() {
        recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .description("Internet Bill")
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        int count = executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("TEST 22: Transaction created correctly with fields")
    void test22_TransactionCreatedCorrectly() {
        RecurringTransaction template = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .description("Internet Bill")
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));

        List<Transaction> txList = transactionRepository.findAllByUserId(userA.getId());
        assertThat(txList).hasSize(1);
        Transaction tx = txList.get(0);
        assertThat(tx.getAmount()).isEqualByComparingTo("2000.00");
        assertThat(tx.getAccount().getId()).isEqualTo(accountA.getId());
        assertThat(tx.getCategory().getId()).isEqualTo(categoryFoodA.getId());
        assertThat(tx.getTransactionDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(tx.getDescription()).isEqualTo("Internet Bill");
        assertThat(tx.getClientOperationId()).isEqualTo("recurring:" + template.getId() + ":2026-09-01");
    }

    @Test
    @DisplayName("TEST 23: Account balance updated accurately after execution")
    void test23_AccountBalanceUpdated() {
        // Account balance initial: 100,000. EXPENSE 5,000 -> 95,000
        recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("5000.00"))
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));

        Account updatedAccount = accountRepository.findById(accountA.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("95000.0000");
    }

    @Test
    @DisplayName("TEST 24: nextRunDate advances after execution")
    void test24_NextRunDateAdvances() {
        RecurringTransaction template = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));

        RecurringTransaction updatedTemplate = recurringTransactionRepository.findById(template.getId()).orElseThrow();
        assertThat(updatedTemplate.getNextRunDate()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    @DisplayName("TEST 25: EndDate prevents further execution and deactivates template")
    void test25_EndDatePreventsFurtherExecution() {
        RecurringTransaction template = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 15)) // endDate before Oct 1
                .active(true)
                .build());

        // Process Sep 1 occurrence
        executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));

        RecurringTransaction updatedTemplate = recurringTransactionRepository.findById(template.getId()).orElseThrow();
        assertThat(updatedTemplate.getNextRunDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(updatedTemplate.isActive()).isFalse(); // Deactivated because nextRun > endDate
    }

    @Test
    @DisplayName("TEST 26: Monthly recurrence frequency calculation (+1 month)")
    void test26_MonthlyRecurrence() {
        RecurringTransaction template = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 1, 31)).nextRunDate(LocalDate.of(2026, 1, 31))
                .active(true)
                .build());

        executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 1, 31));

        RecurringTransaction updated = recurringTransactionRepository.findById(template.getId()).orElseThrow();
        assertThat(updated.getNextRunDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("TEST 27: Weekly recurrence frequency calculation (+7 days)")
    void test27_WeeklyRecurrence() {
        RecurringTransaction template = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("500.00"))
                .frequency(RecurrenceFrequency.WEEKLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));

        RecurringTransaction updated = recurringTransactionRepository.findById(template.getId()).orElseThrow();
        assertThat(updated.getNextRunDate()).isEqualTo(LocalDate.of(2026, 9, 8));
    }

    @Test
    @DisplayName("TEST 28: Daily recurrence frequency calculation (+1 day)")
    void test28_DailyRecurrence() {
        RecurringTransaction template = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("100.00"))
                .frequency(RecurrenceFrequency.DAILY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));

        RecurringTransaction updated = recurringTransactionRepository.findById(template.getId()).orElseThrow();
        assertThat(updated.getNextRunDate()).isEqualTo(LocalDate.of(2026, 9, 2));
    }

    @Test
    @DisplayName("TEST 29: Yearly recurrence frequency calculation (+1 year)")
    void test29_YearlyRecurrence() {
        RecurringTransaction template = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("12000.00"))
                .frequency(RecurrenceFrequency.YEARLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));

        RecurringTransaction updated = recurringTransactionRepository.findById(template.getId()).orElseThrow();
        assertThat(updated.getNextRunDate()).isEqualTo(LocalDate.of(2027, 9, 1));
    }

    @Test
    @DisplayName("TEST 30: Duplicate execution does not duplicate transaction (Idempotency)")
    void test30_DuplicateExecution_DoesNotDuplicateTransaction() {
        RecurringTransaction template = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        // First execution
        executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));
        assertThat(transactionRepository.findAllByUserId(userA.getId())).hasSize(1);

        // Reset template nextRunDate back to Sep 1 to simulate duplicate trigger
        template.setNextRunDate(LocalDate.of(2026, 9, 1));
        recurringTransactionRepository.save(template);

        // Second execution
        executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));
        assertThat(transactionRepository.findAllByUserId(userA.getId())).hasSize(1);
    }

    @Test
    @DisplayName("TEST 31: Duplicate execution does not duplicate balance change")
    void test31_DuplicateExecution_DoesNotDuplicateBalanceChange() {
        RecurringTransaction template = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("5000.00"))
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        // Initial balance 100,000 -> 95,000
        executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));

        Account accountAfterFirst = accountRepository.findById(accountA.getId()).orElseThrow();
        assertThat(accountAfterFirst.getBalance()).isEqualByComparingTo("95000.0000");

        // Simulate retry with same nextRunDate
        template.setNextRunDate(LocalDate.of(2026, 9, 1));
        recurringTransactionRepository.save(template);

        executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));

        Account accountAfterSecond = accountRepository.findById(accountA.getId()).orElseThrow();
        assertThat(accountAfterSecond.getBalance()).isEqualByComparingTo("95000.0000");
    }

    @Test
    @DisplayName("TEST 32: Multiple due occurrences handled safely")
    void test32_MultipleDueOccurrencesHandled() {
        // Daily recurring starting 2026-09-01, executing on 2026-09-03 (3 due days)
        recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("100.00"))
                .frequency(RecurrenceFrequency.DAILY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        int count = executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 3));
        assertThat(count).isEqualTo(3);
        assertThat(transactionRepository.findAllByUserId(userA.getId())).hasSize(3);
    }

    @Test
    @DisplayName("TEST 33: Catch-up limit enforced (capped at 12 occurrences max per run)")
    void test33_CatchUpLimitEnforced() {
        // Daily recurring overdue by 20 days
        RecurringTransaction template = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("100.00"))
                .frequency(RecurrenceFrequency.DAILY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        int count = executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 20));
        assertThat(count).isEqualTo(12); // Capped at MAX_CATCH_UP_LIMIT (12)
        assertThat(transactionRepository.findAllByUserId(userA.getId())).hasSize(12);

        RecurringTransaction updated = recurringTransactionRepository.findById(template.getId()).orElseThrow();
        assertThat(updated.getNextRunDate()).isEqualTo(LocalDate.of(2026, 9, 13));
    }

    @Test
    @DisplayName("TEST 34: Non-owner cannot manually execute (POST /{id}/execute -> 403 Forbidden)")
    void test34_NonOwnerCannotManuallyExecute() throws Exception {
        RecurringTransaction template = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        mockMvc.perform(post("/api/v1/recurring-transactions/" + template.getId() + "/execute")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 35: Concurrent execution safety")
    void test35_ConcurrentExecutionSafety() throws Exception {
        RecurringTransaction template = recurringTransactionRepository.save(RecurringTransaction.builder()
                .user(userA).account(accountA).category(categoryFoodA)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 9, 1)).nextRunDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        int numberOfThreads = 2;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                try {
                    executionService.executeAllDueRecurringTransactionsForDate(LocalDate.of(2026, 9, 1));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        service.shutdown();

        List<Transaction> txList = transactionRepository.findAllByUserId(userA.getId());
        assertThat(txList).hasSize(1);
    }
}
