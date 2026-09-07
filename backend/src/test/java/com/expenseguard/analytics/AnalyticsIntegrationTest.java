package com.expenseguard.analytics;

import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.auth.service.AuthService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration test suite for Phase 4.2 Analytics & Financial Insights module.
 * Validates 30 key scenarios covering financial summaries, projections, validation, security, and edge cases.
 */
import com.expenseguard.ExpenseGuardApplication;

@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsIntegrationTest {

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
    private Account accountA1;
    private Account accountA2;
    private Category catFood;
    private Category catTravel;
    private Category catSalary;

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
                .email("user.a@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(registerA);
        userA = userRepository.findByEmail("user.a@example.com").orElseThrow();
        tokenA = jwtService.generateToken(userA);

        // 2. Register User B
        RegisterRequest registerB = RegisterRequest.builder()
                .name("User B")
                .email("user.b@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(registerB);
        userB = userRepository.findByEmail("user.b@example.com").orElseThrow();
        tokenB = jwtService.generateToken(userB);

        // 3. Accounts for User A
        accountA1 = accountRepository.save(Account.builder()
                .user(userA)
                .name("HDFC Checking")
                .type(AccountType.BANK)
                .balance(new BigDecimal("100000.0000"))
                .currency("INR")
                .build());

        accountA2 = accountRepository.save(Account.builder()
                .user(userA)
                .name("Credit Card")
                .type(AccountType.CREDIT_CARD)
                .balance(new BigDecimal("20000.0000"))
                .currency("INR")
                .build());

        // 4. Categories for User A
        catFood = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Food & Dining")
                .type(CategoryType.EXPENSE)
                .build());

        catTravel = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Travel & Fuel")
                .type(CategoryType.EXPENSE)
                .build());

        catSalary = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Monthly Salary")
                .type(CategoryType.INCOME)
                .build());
    }

    // --- 1. Monthly Analytics Tests ---

    @Test
    @DisplayName("TEST 1: Get monthly summary for valid month (200 OK)")
    void test1_GetMonthlyAnalytics_ReturnsCorrectTotals() throws Exception {
        // Income: 50,000 on Sep 1
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catSalary)
                .type(TransactionType.INCOME).amount(new BigDecimal("50000.00"))
                .transactionDate(LocalDate.of(2026, 9, 1)).build());

        // Expenses: 10,000 Food + 5,000 Travel = 15,000
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("10000.00"))
                .transactionDate(LocalDate.of(2026, 9, 5)).build());

        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA2).category(catTravel)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("5000.00"))
                .transactionDate(LocalDate.of(2026, 9, 15)).build());

        mockMvc.perform(get("/api/v1/analytics/monthly?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month", is("2026-09")))
                .andExpect(jsonPath("$.totalIncome", is(50000.00)))
                .andExpect(jsonPath("$.totalExpense", is(15000.00)))
                .andExpect(jsonPath("$.netSavings", is(35000.00)))
                .andExpect(jsonPath("$.savingsRate", is(70.00)));
    }

    @Test
    @DisplayName("TEST 2: Monthly summary with no transactions returns zero values (200 OK)")
    void test2_GetMonthlyAnalytics_NoTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/monthly?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month", is("2026-09")))
                .andExpect(jsonPath("$.totalIncome", is(0.0)))
                .andExpect(jsonPath("$.totalExpense", is(0.0)))
                .andExpect(jsonPath("$.netSavings", is(0.0)))
                .andExpect(jsonPath("$.savingsRate", is(0.0)));
    }

    @Test
    @DisplayName("TEST 3: Monthly summary with zero income handles savings rate safely (200 OK)")
    void test3_GetMonthlyAnalytics_ZeroIncome() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .transactionDate(LocalDate.of(2026, 9, 10)).build());

        mockMvc.perform(get("/api/v1/analytics/monthly?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome", is(0.0)))
                .andExpect(jsonPath("$.totalExpense", is(2000.00)))
                .andExpect(jsonPath("$.netSavings", is(-2000.00)))
                .andExpect(jsonPath("$.savingsRate", is(0.0)));
    }

    @Test
    @DisplayName("TEST 4: Default to current month when month query param is omitted")
    void test4_GetMonthlyAnalytics_OmittedMonthDefaultsToCurrent() throws Exception {
        String currentMonth = YearMonth.now().toString();
        mockMvc.perform(get("/api/v1/analytics/monthly")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month", is(currentMonth)));
    }

    @Test
    @DisplayName("TEST 5: Invalid month format returns 400 Bad Request")
    void test5_GetMonthlyAnalytics_InvalidMonthFormat() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/monthly?month=2026-13")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    // --- 2. Category Analytics Tests ---

    @Test
    @DisplayName("TEST 6: Get category expense breakdown with percentages (200 OK)")
    void test6_GetCategoryAnalytics_Success() throws Exception {
        // Food: 6,000 (60%), Travel: 4,000 (40%) Total: 10,000
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("6000.00"))
                .transactionDate(LocalDate.of(2026, 9, 2)).build());

        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catTravel)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("4000.00"))
                .transactionDate(LocalDate.of(2026, 9, 8)).build());

        mockMvc.perform(get("/api/v1/analytics/categories?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month", is("2026-09")))
                .andExpect(jsonPath("$.totalExpense", is(10000.00)))
                .andExpect(jsonPath("$.categories", hasSize(2)))
                .andExpect(jsonPath("$.categories[0].categoryName", is("Food & Dining")))
                .andExpect(jsonPath("$.categories[0].amount", is(6000.00)))
                .andExpect(jsonPath("$.categories[0].percentage", is(60.00)))
                .andExpect(jsonPath("$.categories[1].categoryName", is("Travel & Fuel")))
                .andExpect(jsonPath("$.categories[1].amount", is(4000.00)))
                .andExpect(jsonPath("$.categories[1].percentage", is(40.00)));
    }

    @Test
    @DisplayName("TEST 7: Category breakdown ordered by expense amount descending")
    void test7_GetCategoryAnalytics_SortedDescending() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catTravel)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("8000.00"))
                .transactionDate(LocalDate.of(2026, 9, 2)).build());

        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("2000.00"))
                .transactionDate(LocalDate.of(2026, 9, 8)).build());

        mockMvc.perform(get("/api/v1/analytics/categories?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].categoryName", is("Travel & Fuel")))
                .andExpect(jsonPath("$.categories[1].categoryName", is("Food & Dining")));
    }

    @Test
    @DisplayName("TEST 8: Category breakdown with no expenses returns empty category list")
    void test8_GetCategoryAnalytics_NoExpenses() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/categories?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense", is(0.0)))
                .andExpect(jsonPath("$.categories", hasSize(0)));
    }

    @Test
    @DisplayName("TEST 9: Income transactions excluded from category expense breakdown")
    void test9_GetCategoryAnalytics_ExcludesIncome() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catSalary)
                .type(TransactionType.INCOME).amount(new BigDecimal("50000.00"))
                .transactionDate(LocalDate.of(2026, 9, 1)).build());

        mockMvc.perform(get("/api/v1/analytics/categories?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense", is(0.0)))
                .andExpect(jsonPath("$.categories", hasSize(0)));
    }

    @Test
    @DisplayName("TEST 10: Invalid month string in category analytics returns 400 Bad Request")
    void test10_GetCategoryAnalytics_InvalidMonth() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/categories?month=invalid")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    // --- 3. Account Analytics Tests ---

    @Test
    @DisplayName("TEST 11: Get account cashflow breakdown (200 OK)")
    void test11_GetAccountAnalytics_Success() throws Exception {
        // Account A1: Income 50k, Expense 10k -> Net +40k
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catSalary)
                .type(TransactionType.INCOME).amount(new BigDecimal("50000.00"))
                .transactionDate(LocalDate.of(2026, 9, 1)).build());

        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("10000.00"))
                .transactionDate(LocalDate.of(2026, 9, 5)).build());

        // Account A2: Expense 5k -> Net -5k
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA2).category(catTravel)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("5000.00"))
                .transactionDate(LocalDate.of(2026, 9, 15)).build());

        mockMvc.perform(get("/api/v1/analytics/accounts?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month", is("2026-09")))
                .andExpect(jsonPath("$.accounts", hasSize(2)));
    }

    @Test
    @DisplayName("TEST 12: Account analytics with no transactions returns empty list")
    void test12_GetAccountAnalytics_NoTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/accounts?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts", hasSize(0)));
    }

    @Test
    @DisplayName("TEST 13: Accounts with both income and expense calculate net change accurately")
    void test13_GetAccountAnalytics_NetChangeCalculation() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catSalary)
                .type(TransactionType.INCOME).amount(new BigDecimal("30000.00"))
                .transactionDate(LocalDate.of(2026, 9, 1)).build());

        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("12000.00"))
                .transactionDate(LocalDate.of(2026, 9, 5)).build());

        mockMvc.perform(get("/api/v1/analytics/accounts?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[0].totalIncome", is(30000.00)))
                .andExpect(jsonPath("$.accounts[0].totalExpense", is(12000.00)))
                .andExpect(jsonPath("$.accounts[0].netChange", is(18000.00)));
    }

    @Test
    @DisplayName("TEST 14: Default month for account analytics")
    void test14_GetAccountAnalytics_DefaultMonth() throws Exception {
        String currentMonth = YearMonth.now().toString();
        mockMvc.perform(get("/api/v1/analytics/accounts")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month", is(currentMonth)));
    }

    @Test
    @DisplayName("TEST 15: Invalid month string in account analytics returns 400 Bad Request")
    void test15_GetAccountAnalytics_InvalidMonth() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/accounts?month=2026-99")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    // --- 4. Top Categories Tests ---

    @Test
    @DisplayName("TEST 16: Get top 5 categories by default (200 OK)")
    void test16_GetTopCategories_DefaultLimit5() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("6000.00"))
                .transactionDate(LocalDate.of(2026, 9, 2)).build());

        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catTravel)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("4000.00"))
                .transactionDate(LocalDate.of(2026, 9, 8)).build());

        mockMvc.perform(get("/api/v1/analytics/top-categories?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topCategories", hasSize(2)))
                .andExpect(jsonPath("$.topCategories[0].categoryName", is("Food & Dining")));
    }

    @Test
    @DisplayName("TEST 17: Limit parameter caps returned top categories (limit=1)")
    void test17_GetTopCategories_WithLimit1() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("6000.00"))
                .transactionDate(LocalDate.of(2026, 9, 2)).build());

        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catTravel)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("4000.00"))
                .transactionDate(LocalDate.of(2026, 9, 8)).build());

        mockMvc.perform(get("/api/v1/analytics/top-categories?month=2026-09&limit=1")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topCategories", hasSize(1)))
                .andExpect(jsonPath("$.topCategories[0].categoryName", is("Food & Dining")));
    }

    @Test
    @DisplayName("TEST 18: Limit parameter out of bounds (<1 or >20) returns 400 Bad Request")
    void test18_GetTopCategories_LimitOutOfBounds() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/top-categories?month=2026-09&limit=0")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/analytics/top-categories?month=2026-09&limit=25")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 19: Top categories with zero expenses returns empty list")
    void test19_GetTopCategories_NoExpenses() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/top-categories?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topCategories", hasSize(0)));
    }

    @Test
    @DisplayName("TEST 20: Invalid month format in top-categories returns 400 Bad Request")
    void test20_GetTopCategories_InvalidMonth() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/top-categories?month=BAD_MONTH")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    // --- 5. Budget Performance Tests ---

    @Test
    @DisplayName("TEST 21: Get budget performance summary (200 OK)")
    void test21_GetBudgetPerformance_Success() throws Exception {
        // Create 2 budgets for User A in Sep 2026
        budgetRepository.save(Budget.builder()
                .user(userA).category(catFood)
                .amount(new BigDecimal("10000.00")).month("2026-09").build());

        budgetRepository.save(Budget.builder()
                .user(userA).category(catTravel)
                .amount(new BigDecimal("5000.00")).month("2026-09").build());

        // Add spending for Food (6,000) and Travel (3,000) -> Total Spent = 9,000 / Total Budgeted = 15,000 (60%)
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("6000.00"))
                .transactionDate(LocalDate.of(2026, 9, 2)).build());

        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catTravel)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("3000.00"))
                .transactionDate(LocalDate.of(2026, 9, 8)).build());

        mockMvc.perform(get("/api/v1/analytics/budget-performance?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month", is("2026-09")))
                .andExpect(jsonPath("$.budgets", hasSize(2)))
                .andExpect(jsonPath("$.totalBudgeted", is(15000.00)))
                .andExpect(jsonPath("$.totalSpent", is(9000.00)))
                .andExpect(jsonPath("$.overallUtilizationPercentage", is(60.00)));
    }

    @Test
    @DisplayName("TEST 22: Budget performance with no budgets returns zero totals and 0.00 utilization")
    void test22_GetBudgetPerformance_NoBudgets() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/budget-performance?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgets", hasSize(0)))
                .andExpect(jsonPath("$.totalBudgeted", is(0.0)))
                .andExpect(jsonPath("$.totalSpent", is(0.0)))
                .andExpect(jsonPath("$.overallUtilizationPercentage", is(0.0)));
    }

    @Test
    @DisplayName("TEST 23: Budget performance calculates overall utilization percentage accurately")
    void test23_GetBudgetPerformance_UtilizationAccuracy() throws Exception {
        budgetRepository.save(Budget.builder()
                .user(userA).category(catFood)
                .amount(new BigDecimal("20000.00")).month("2026-09").build());

        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("15000.00"))
                .transactionDate(LocalDate.of(2026, 9, 10)).build());

        mockMvc.perform(get("/api/v1/analytics/budget-performance?month=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBudgeted", is(20000.00)))
                .andExpect(jsonPath("$.totalSpent", is(15000.00)))
                .andExpect(jsonPath("$.overallUtilizationPercentage", is(75.00)));
    }

    @Test
    @DisplayName("TEST 24: Invalid month format in budget performance returns 400 Bad Request")
    void test24_GetBudgetPerformance_InvalidMonth() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/budget-performance?month=2026-14")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    // --- 6. Monthly Trend Tests ---

    @Test
    @DisplayName("TEST 25: Get monthly trend over multi-month range (from=2026-07 to 2026-09) (200 OK)")
    void test25_GetMonthlyTrend_MultiMonthSuccess() throws Exception {
        // Jul 2026: Income 40k, Expense 10k
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catSalary)
                .type(TransactionType.INCOME).amount(new BigDecimal("40000.00"))
                .transactionDate(LocalDate.of(2026, 7, 1)).build());

        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("10000.00"))
                .transactionDate(LocalDate.of(2026, 7, 10)).build());

        // Aug 2026: Income 45k, Expense 15k
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catSalary)
                .type(TransactionType.INCOME).amount(new BigDecimal("45000.00"))
                .transactionDate(LocalDate.of(2026, 8, 1)).build());

        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("15000.00"))
                .transactionDate(LocalDate.of(2026, 8, 12)).build());

        // Sep 2026: Income 50k, Expense 20k
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catSalary)
                .type(TransactionType.INCOME).amount(new BigDecimal("50000.00"))
                .transactionDate(LocalDate.of(2026, 9, 1)).build());

        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("20000.00"))
                .transactionDate(LocalDate.of(2026, 9, 15)).build());

        mockMvc.perform(get("/api/v1/analytics/trend?from=2026-07&to=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromMonth", is("2026-07")))
                .andExpect(jsonPath("$.toMonth", is("2026-09")))
                .andExpect(jsonPath("$.trends", hasSize(3)))
                .andExpect(jsonPath("$.trends[0].month", is("2026-07")))
                .andExpect(jsonPath("$.trends[0].netSavings", is(30000.00)))
                .andExpect(jsonPath("$.trends[1].month", is("2026-08")))
                .andExpect(jsonPath("$.trends[1].netSavings", is(30000.00)))
                .andExpect(jsonPath("$.trends[2].month", is("2026-09")))
                .andExpect(jsonPath("$.trends[2].netSavings", is(30000.00)));
    }

    @Test
    @DisplayName("TEST 26: Default trend returns 6 months range up to current month")
    void test26_GetMonthlyTrend_Default6Months() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trend")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trends", hasSize(6)));
    }

    @Test
    @DisplayName("TEST 27: Invalid trend range where from > to returns 400 Bad Request")
    void test27_GetMonthlyTrend_FromAfterTo_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trend?from=2026-09&to=2026-05")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 28: Date range exceeding 24 months returns 400 Bad Request")
    void test28_GetMonthlyTrend_Exceeds24Months_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trend?from=2024-01&to=2026-09") // 33 months
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 29: Invalid month format in trend range returns 400 Bad Request")
    void test29_GetMonthlyTrend_InvalidFormat_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trend?from=2026/01&to=2026-09")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    // --- 7. Cross-User Data Security & Authentication ---

    @Test
    @DisplayName("TEST 30: Security - Cross-user data isolation and authentication enforcement")
    void test30_Security_CrossUserIsolation() throws Exception {
        // User A data
        transactionRepository.save(Transaction.builder()
                .user(userA).account(accountA1).category(catFood)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("10000.00"))
                .transactionDate(LocalDate.of(2026, 9, 5)).build());

        // User B account, category & transaction
        Account accountB = accountRepository.save(Account.builder()
                .user(userB).name("B Cash").type(AccountType.CASH)
                .balance(new BigDecimal("5000.0000")).currency("INR").build());

        Category catB = categoryRepository.save(Category.builder()
                .user(userB).name("Shopping B").type(CategoryType.EXPENSE).build());

        transactionRepository.save(Transaction.builder()
                .user(userB).account(accountB).category(catB)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("50000.00"))
                .transactionDate(LocalDate.of(2026, 9, 5)).build());

        // User B queries analytics -> gets only User B's 50k expense, not User A's 10k
        mockMvc.perform(get("/api/v1/analytics/monthly?month=2026-09")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense", is(50000.00)));

        // Unauthenticated request returns 401 Unauthorized
        mockMvc.perform(get("/api/v1/analytics/monthly?month=2026-09"))
                .andExpect(status().isUnauthorized());
    }
}
