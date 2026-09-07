package com.expenseguard.budget;

import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.auth.service.AuthService;
import com.expenseguard.budget.dto.BudgetRequest;
import com.expenseguard.budget.entity.Budget;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test suite verifying Phase 4.1 Budget Management requirements.
 * Covers 30 comprehensive test scenarios for Budget creation, security, utilization math, and validation.
 */
import com.expenseguard.ExpenseGuardApplication;

@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BudgetIntegrationTest {

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
    private Category userAExpenseCatFood;
    private Category userAExpenseCatTravel;
    private Category userAIncomeCat;
    private Category userBExpenseCat;

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

        // 3. Accounts
        userAAccount = accountRepository.save(Account.builder()
                .user(userA)
                .name("Checking Account")
                .type(AccountType.BANK)
                .balance(new BigDecimal("50000.0000"))
                .currency("INR")
                .build());

        // 4. Categories for User A
        userAExpenseCatFood = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Food")
                .type(CategoryType.EXPENSE)
                .build());

        userAExpenseCatTravel = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Travel")
                .type(CategoryType.EXPENSE)
                .build());

        userAIncomeCat = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Salary")
                .type(CategoryType.INCOME)
                .build());

        // 5. Categories for User B
        userBExpenseCat = categoryRepository.save(Category.builder()
                .user(userB)
                .name("Shopping")
                .type(CategoryType.EXPENSE)
                .build());
    }

    @Test
    @DisplayName("TEST 1: Create expense budget (201 Created)")
    void test1_CreateExpenseBudget_Returns201() throws Exception {
        BudgetRequest req = BudgetRequest.builder()
                .categoryId(userAExpenseCatFood.getId())
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.categoryId", is(userAExpenseCatFood.getId().toString())))
                .andExpect(jsonPath("$.categoryName", is("Food")))
                .andExpect(jsonPath("$.month", is("2026-09")))
                .andExpect(jsonPath("$.amount", is(10000.00)))
                .andExpect(jsonPath("$.spentAmount", is(0)))
                .andExpect(jsonPath("$.remainingAmount", is(10000.00)))
                .andExpect(jsonPath("$.utilizationPercentage", is(0.0)));
    }

    @Test
    @DisplayName("TEST 2: Get all own budgets (200 OK)")
    void test2_GetAllOwnBudgets_ReturnsUserABudgets() throws Exception {
        budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        mockMvc.perform(get("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].categoryName", is("Food")));
    }

    @Test
    @DisplayName("TEST 3: Get own budget by ID (200 OK)")
    void test3_GetOwnBudget_Returns200() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        mockMvc.perform(get("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(budget.getId().toString())))
                .andExpect(jsonPath("$.categoryName", is("Food")));
    }

    @Test
    @DisplayName("TEST 4: Update own budget (200 OK)")
    void test4_UpdateOwnBudget_Returns200() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        BudgetRequest updateReq = BudgetRequest.builder()
                .categoryId(userAExpenseCatFood.getId())
                .amount(new BigDecimal("15000.00"))
                .month("2026-09")
                .build();

        mockMvc.perform(put("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(15000.00)));
    }

    @Test
    @DisplayName("TEST 5: Delete own budget (204 No Content)")
    void test5_DeleteOwnBudget_Returns204() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        mockMvc.perform(delete("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        assertThat(budgetRepository.findById(budget.getId())).isEmpty();
    }

    @Test
    @DisplayName("TEST 6: Filter budgets by month (?month=2026-09)")
    void test6_FilterByMonth_ReturnsOnlyMatchingMonthBudgets() throws Exception {
        budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("8000.00"))
                .month("2026-10")
                .build());

        mockMvc.perform(get("/api/v1/budgets?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].month", is("2026-09")));
    }

    @Test
    @DisplayName("TEST 7: Cross-user budget access returns 403 Forbidden")
    void test7_CrossUserBudgetAccess_Returns403() throws Exception {
        Budget budgetA = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        mockMvc.perform(get("/api/v1/budgets/" + budgetA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 8: Cross-user budget update returns 403 Forbidden")
    void test8_CrossUserBudgetUpdate_Returns403() throws Exception {
        Budget budgetA = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        BudgetRequest updateReq = BudgetRequest.builder()
                .categoryId(userBExpenseCat.getId())
                .amount(new BigDecimal("9999.00"))
                .month("2026-09")
                .build();

        mockMvc.perform(put("/api/v1/budgets/" + budgetA.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 9: Cross-user budget delete returns 403 Forbidden")
    void test9_CrossUserBudgetDelete_Returns403() throws Exception {
        Budget budgetA = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        mockMvc.perform(delete("/api/v1/budgets/" + budgetA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 10: Cross-user category reference returns 403 Forbidden")
    void test10_CrossUserCategoryReference_Returns403() throws Exception {
        BudgetRequest req = BudgetRequest.builder()
                .categoryId(userBExpenseCat.getId()) // User B category
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 11: No JWT returns 401 Unauthorized")
    void test11_NoJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/budgets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TEST 12: Invalid amount format returns 400 Bad Request")
    void test12_InvalidAmountFormat_Returns400() throws Exception {
        String json = String.format("""
                {
                  "categoryId": "%s",
                  "amount": "INVALID",
                  "month": "2026-09"
                }
                """, userAExpenseCatFood.getId());

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 13: Zero amount (0) returns 400 Bad Request")
    void test13_ZeroAmount_Returns400() throws Exception {
        BudgetRequest req = BudgetRequest.builder()
                .categoryId(userAExpenseCatFood.getId())
                .amount(BigDecimal.ZERO)
                .month("2026-09")
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 14: Negative amount returns 400 Bad Request")
    void test14_NegativeAmount_Returns400() throws Exception {
        BudgetRequest req = BudgetRequest.builder()
                .categoryId(userAExpenseCatFood.getId())
                .amount(new BigDecimal("-500.00"))
                .month("2026-09")
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 15: Missing category returns 400 Bad Request")
    void test15_MissingCategory_Returns400() throws Exception {
        BudgetRequest req = BudgetRequest.builder()
                .categoryId(null)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 16: Missing month returns 400 Bad Request")
    void test16_MissingMonth_Returns400() throws Exception {
        BudgetRequest req = BudgetRequest.builder()
                .categoryId(userAExpenseCatFood.getId())
                .amount(new BigDecimal("10000.00"))
                .month(null)
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 17: Invalid month format returns 400 Bad Request")
    void test17_InvalidMonth_Returns400() throws Exception {
        BudgetRequest req = BudgetRequest.builder()
                .categoryId(userAExpenseCatFood.getId())
                .amount(new BigDecimal("10000.00"))
                .month("2026-13") // invalid month 13
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 18: Income category returns 400 Bad Request")
    void test18_IncomeCategory_Returns400() throws Exception {
        BudgetRequest req = BudgetRequest.builder()
                .categoryId(userAIncomeCat.getId()) // INCOME category
                .amount(new BigDecimal("50000.00"))
                .month("2026-09")
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("EXPENSE")));
    }

    @Test
    @DisplayName("TEST 19: Duplicate budget returns 409 Conflict")
    void test19_DuplicateBudget_Returns409() throws Exception {
        budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        BudgetRequest req = BudgetRequest.builder()
                .categoryId(userAExpenseCatFood.getId())
                .amount(new BigDecimal("12000.00"))
                .month("2026-09")
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TEST 20: Client userId spoofing is ignored")
    void test20_ClientUserIdSpoofing_IsIgnored() throws Exception {
        String spoofedJson = String.format("""
                {
                  "categoryId": "%s",
                  "amount": 10000,
                  "month": "2026-09",
                  "userId": "%s"
                }
                """, userAExpenseCatFood.getId(), userB.getId());

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spoofedJson))
                .andExpect(status().isCreated());

        assertThat(budgetRepository.findAllByUserId(userA.getId())).hasSize(1);
        assertThat(budgetRepository.findAllByUserId(userB.getId())).isEmpty();
    }

    @Test
    @DisplayName("TEST 21: Calculate spent amount accuracy")
    void test21_CalculateSpentAmountAccuracy() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        // Add 2 expense transactions for Food in Sep 2026 (2000 + 3000 = 5000)
        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCatFood)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("2000.00"))
                .transactionDate(LocalDate.of(2026, 9, 5))
                .build());

        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCatFood)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("3000.00"))
                .transactionDate(LocalDate.of(2026, 9, 15))
                .build());

        mockMvc.perform(get("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentAmount", is(5000.00)));
    }

    @Test
    @DisplayName("TEST 22: Calculate remaining amount accuracy")
    void test22_CalculateRemainingAmountAccuracy() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCatFood)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("7500.00"))
                .transactionDate(LocalDate.of(2026, 9, 10))
                .build());

        mockMvc.perform(get("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentAmount", is(7500.00)))
                .andExpect(jsonPath("$.remainingAmount", is(2500.00)));
    }

    @Test
    @DisplayName("TEST 23: Calculate utilization percentage accuracy")
    void test23_CalculateUtilizationPercentageAccuracy() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCatFood)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("7500.00"))
                .transactionDate(LocalDate.of(2026, 9, 10))
                .build());

        mockMvc.perform(get("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilizationPercentage", is(75.00)));
    }

    @Test
    @DisplayName("TEST 24: Income transaction excluded from budget spending")
    void test24_IncomeTransaction_ExcludedFromBudgetSpending() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        // Add income transaction for Food category
        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAIncomeCat)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("5000.00"))
                .transactionDate(LocalDate.of(2026, 9, 10))
                .build());

        mockMvc.perform(get("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentAmount", is(0)));
    }

    @Test
    @DisplayName("TEST 25: Different category transaction excluded")
    void test25_DifferentCategoryTransaction_Excluded() throws Exception {
        Budget budgetFood = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        // Travel expense transaction
        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCatTravel)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("4000.00"))
                .transactionDate(LocalDate.of(2026, 9, 10))
                .build());

        mockMvc.perform(get("/api/v1/budgets/" + budgetFood.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentAmount", is(0)));
    }

    @Test
    @DisplayName("TEST 26: Different month transaction excluded")
    void test26_DifferentMonthTransaction_Excluded() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        // October transaction
        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCatFood)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("3000.00"))
                .transactionDate(LocalDate.of(2026, 10, 5))
                .build());

        mockMvc.perform(get("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentAmount", is(0)));
    }

    @Test
    @DisplayName("TEST 27: Other user's transactions excluded")
    void test27_OtherUserTransactions_Excluded() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        // User B transaction
        transactionRepository.save(Transaction.builder()
                .user(userB)
                .account(accountRepository.save(Account.builder().user(userB).name("B Acc").type(AccountType.CASH).balance(new BigDecimal("1000")).currency("INR").build()))
                .category(userBExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("2000.00"))
                .transactionDate(LocalDate.of(2026, 9, 10))
                .build());

        mockMvc.perform(get("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentAmount", is(0)));
    }

    @Test
    @DisplayName("TEST 28: Update budget amount recalculates remaining and utilization")
    void test28_UpdateBudgetAmount_RecalculatesRemainingAndUtilization() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCatFood)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("6000.00"))
                .transactionDate(LocalDate.of(2026, 9, 10))
                .build());

        // Initial: Budget 10000, Spent 6000 -> Remaining 4000, Utilization 60%
        mockMvc.perform(get("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingAmount", is(4000.00)))
                .andExpect(jsonPath("$.utilizationPercentage", is(60.00)));

        // Update limit to 15000
        BudgetRequest updateReq = BudgetRequest.builder()
                .categoryId(userAExpenseCatFood.getId())
                .amount(new BigDecimal("15000.00"))
                .month("2026-09")
                .build();

        mockMvc.perform(put("/api/v1/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(15000.00)))
                .andExpect(jsonPath("$.spentAmount", is(6000.00)))
                .andExpect(jsonPath("$.remainingAmount", is(9000.00)))
                .andExpect(jsonPath("$.utilizationPercentage", is(40.00)));
    }

    @Test
    @DisplayName("TEST 29: Nonexistent budget returns 404 Not Found")
    void test29_NonexistentBudget_Returns404() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/budgets/" + randomId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TEST 30: Final budget calculation accuracy (Budget 10000, Expenses 2k+3k+1.5k = Spent 6.5k, Remaining 3.5k, Utilization 65%)")
    void test30_FinalBudgetCalculationAccuracy() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .user(userA)
                .category(userAExpenseCatFood)
                .amount(new BigDecimal("10000.00"))
                .month("2026-09")
                .build());

        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCatFood)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("2000.00"))
                .transactionDate(LocalDate.of(2026, 9, 2))
                .build());

        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCatFood)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("3000.00"))
                .transactionDate(LocalDate.of(2026, 9, 12))
                .build());

        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCatFood)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("1500.00"))
                .transactionDate(LocalDate.of(2026, 9, 22))
                .build());

        mockMvc.perform(get("/api/v1/budgets/" + budget.getId() + "/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount", is(10000.00)))
                .andExpect(jsonPath("$.spentAmount", is(6500.00)))
                .andExpect(jsonPath("$.remainingAmount", is(3500.00)))
                .andExpect(jsonPath("$.utilizationPercentage", is(65.00)));
    }
}
