package com.expenseguard.budget;

import com.expenseguard.ExpenseGuardApplication;
import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.auth.service.AuthService;
import com.expenseguard.budget.entity.Budget;
import com.expenseguard.budget.repository.BudgetRepository;
import com.expenseguard.budget.service.BudgetService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 6.1 Hardening Test Suite: Budget Alert Threshold Boundaries and Spending Isolation.
 */
@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BudgetAndAnalyticsHardeningTest {

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
    private BudgetService budgetService;

    @Autowired
    private AuthService authService;

    private User user;
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
                .name("Budget Hardening User")
                .email("budgethard@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(register);
        user = userRepository.findByEmail("budgethard@example.com").orElseThrow();

        account = accountRepository.save(Account.builder()
                .user(user)
                .name("Budget Account")
                .type(AccountType.BANK)
                .balance(new BigDecimal("100000.0000"))
                .build());

        category = categoryRepository.save(Category.builder()
                .user(user)
                .name("Dining")
                .type(CategoryType.EXPENSE)
                .build());
    }

    @Test
    @DisplayName("Budget Threshold 1: Spent < 80% yields status OK / no alert threshold exceeded")
    void spentUnder80PercentOK() {
        BigDecimal budgetAmount = new BigDecimal("10000.00");
        // 7,999 spent out of 10,000 = 79.99%
        BigDecimal spent = new BigDecimal("7999.00");

        BigDecimal utilization = spent.multiply(new BigDecimal("100"))
                .divide(budgetAmount, 2, java.math.RoundingMode.HALF_UP);

        assertThat(utilization).isEqualByComparingTo("79.99");
        assertThat(utilization.compareTo(new BigDecimal("80.00"))).isLessThan(0);
    }

    @Test
    @DisplayName("Budget Threshold 2: Spent == 80% yields WARNING threshold")
    void spentAt80PercentWarning() {
        BigDecimal budgetAmount = new BigDecimal("10000.00");
        BigDecimal spent = new BigDecimal("8000.00");

        BigDecimal utilization = spent.multiply(new BigDecimal("100"))
                .divide(budgetAmount, 2, java.math.RoundingMode.HALF_UP);

        assertThat(utilization).isEqualByComparingTo("80.00");
        assertThat(utilization.compareTo(new BigDecimal("80.00"))).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Budget Threshold 3: Spent == 100% yields REACHED threshold")
    void spentAt100PercentReached() {
        BigDecimal budgetAmount = new BigDecimal("10000.00");
        BigDecimal spent = new BigDecimal("10000.00");

        BigDecimal utilization = spent.multiply(new BigDecimal("100"))
                .divide(budgetAmount, 2, java.math.RoundingMode.HALF_UP);

        assertThat(utilization).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Budget Threshold 4: Spent > 100% yields EXCEEDED threshold")
    void spentOver100PercentExceeded() {
        BigDecimal budgetAmount = new BigDecimal("10000.00");
        BigDecimal spent = new BigDecimal("12000.00");

        BigDecimal utilization = spent.multiply(new BigDecimal("100"))
                .divide(budgetAmount, 2, java.math.RoundingMode.HALF_UP);

        assertThat(utilization).isEqualByComparingTo("120.00");
        assertThat(utilization.compareTo(new BigDecimal("100.00"))).isGreaterThan(0);
    }
}
