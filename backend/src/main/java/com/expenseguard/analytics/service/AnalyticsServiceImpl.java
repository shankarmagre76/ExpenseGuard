package com.expenseguard.analytics.service;

import com.expenseguard.analytics.dto.*;
import com.expenseguard.analytics.projection.AccountSummaryProjection;
import com.expenseguard.analytics.projection.CategoryExpenseProjection;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.security.CurrentUserService;
import com.expenseguard.budget.dto.BudgetResponse;
import com.expenseguard.budget.service.BudgetService;
import com.expenseguard.transaction.entity.TransactionType;
import com.expenseguard.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of AnalyticsService providing server-side financial calculations
 * strictly scoped to the authenticated user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final CurrentUserService currentUserService;

    @Override
    public MonthlyAnalyticsResponse getMonthlyAnalytics(String monthStr) {
        User currentUser = currentUserService.getCurrentUser();
        YearMonth ym = parseYearMonthOrDefault(monthStr);
        return calculateMonthlyAnalytics(currentUser.getId(), ym);
    }

    @Override
    public CategoryAnalyticsResponse getCategoryAnalytics(String monthStr) {
        User currentUser = currentUserService.getCurrentUser();
        YearMonth ym = parseYearMonthOrDefault(monthStr);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        BigDecimal totalExpense = transactionRepository.calculateTotalByUserIdAndTypeAndDateRange(
                currentUser.getId(), TransactionType.EXPENSE, startDate, endDate
        );
        if (totalExpense == null) {
            totalExpense = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            totalExpense = totalExpense.setScale(2, RoundingMode.HALF_UP);
        }

        List<CategoryExpenseProjection> projections = transactionRepository
                .findCategoryExpensesByUserIdAndDateRange(currentUser.getId(), startDate, endDate);

        BigDecimal finalTotalExpense = totalExpense;
        List<CategoryAnalyticsResponse.CategoryExpenseItem> items = projections.stream()
                .map(p -> {
                    BigDecimal amount = p.getAmount() != null ? p.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    BigDecimal percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    if (finalTotalExpense.compareTo(BigDecimal.ZERO) > 0) {
                        percentage = amount.multiply(BigDecimal.valueOf(100))
                                .divide(finalTotalExpense, 2, RoundingMode.HALF_UP);
                    }
                    return CategoryAnalyticsResponse.CategoryExpenseItem.builder()
                            .categoryId(p.getCategoryId())
                            .categoryName(p.getCategoryName())
                            .amount(amount)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());

        return CategoryAnalyticsResponse.builder()
                .month(ym.toString())
                .totalExpense(totalExpense)
                .categories(items)
                .build();
    }

    @Override
    public AccountAnalyticsResponse getAccountAnalytics(String monthStr) {
        User currentUser = currentUserService.getCurrentUser();
        YearMonth ym = parseYearMonthOrDefault(monthStr);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<AccountSummaryProjection> projections = transactionRepository
                .findAccountSummariesByUserIdAndDateRange(currentUser.getId(), startDate, endDate);

        List<AccountAnalyticsResponse.AccountCashflowItem> items = projections.stream()
                .map(p -> {
                    BigDecimal inc = p.getTotalIncome() != null ? p.getTotalIncome().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    BigDecimal exp = p.getTotalExpense() != null ? p.getTotalExpense().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    BigDecimal netChange = inc.subtract(exp).setScale(2, RoundingMode.HALF_UP);
                    return AccountAnalyticsResponse.AccountCashflowItem.builder()
                            .accountId(p.getAccountId())
                            .accountName(p.getAccountName())
                            .totalIncome(inc)
                            .totalExpense(exp)
                            .netChange(netChange)
                            .build();
                })
                .collect(Collectors.toList());

        return AccountAnalyticsResponse.builder()
                .month(ym.toString())
                .accounts(items)
                .build();
    }

    @Override
    public TopCategoryResponse getTopCategories(String monthStr, Integer limit) {
        int finalLimit = (limit == null) ? 5 : limit;
        if (finalLimit < 1 || finalLimit > 20) {
            throw new IllegalArgumentException("Limit must be between 1 and 20");
        }

        CategoryAnalyticsResponse categoryAnalytics = getCategoryAnalytics(monthStr);
        List<CategoryAnalyticsResponse.CategoryExpenseItem> topCategories = categoryAnalytics.getCategories().stream()
                .limit(finalLimit)
                .collect(Collectors.toList());

        return TopCategoryResponse.builder()
                .month(categoryAnalytics.getMonth())
                .topCategories(topCategories)
                .build();
    }

    @Override
    public BudgetPerformanceResponse getBudgetPerformance(String monthStr) {
        YearMonth ym = parseYearMonthOrDefault(monthStr);
        String formattedMonth = ym.toString();

        List<BudgetResponse> budgets = budgetService.getAllBudgetsForCurrentUser(formattedMonth);

        BigDecimal totalBudgeted = budgets.stream()
                .map(b -> b.getAmount() != null ? b.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalSpent = budgets.stream()
                .map(b -> b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal overallUtilization = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (totalBudgeted.compareTo(BigDecimal.ZERO) > 0) {
            overallUtilization = totalSpent.multiply(BigDecimal.valueOf(100))
                    .divide(totalBudgeted, 2, RoundingMode.HALF_UP);
        }

        return BudgetPerformanceResponse.builder()
                .month(formattedMonth)
                .budgets(budgets)
                .totalBudgeted(totalBudgeted)
                .totalSpent(totalSpent)
                .overallUtilizationPercentage(overallUtilization)
                .build();
    }

    @Override
    public MonthlyTrendResponse getMonthlyTrend(String fromMonthStr, String toMonthStr) {
        User currentUser = currentUserService.getCurrentUser();
        YearMonth endYM = (toMonthStr != null && !toMonthStr.trim().isEmpty())
                ? parseYearMonth(toMonthStr)
                : YearMonth.now();

        YearMonth startYM = (fromMonthStr != null && !fromMonthStr.trim().isEmpty())
                ? parseYearMonth(fromMonthStr)
                : endYM.minusMonths(5);

        if (startYM.isAfter(endYM)) {
            throw new IllegalArgumentException("fromMonth cannot be after toMonth");
        }

        long monthsCount = ChronoUnit.MONTHS.between(startYM, endYM) + 1;
        if (monthsCount > 24) {
            throw new IllegalArgumentException("Date range cannot exceed 24 months");
        }

        List<MonthlyTrendResponse.MonthlyTrendItem> trendItems = new ArrayList<>();
        YearMonth current = startYM;
        while (!current.isAfter(endYM)) {
            MonthlyAnalyticsResponse analytics = calculateMonthlyAnalytics(currentUser.getId(), current);
            trendItems.add(MonthlyTrendResponse.MonthlyTrendItem.builder()
                    .month(current.toString())
                    .totalIncome(analytics.getTotalIncome())
                    .totalExpense(analytics.getTotalExpense())
                    .netSavings(analytics.getNetSavings())
                    .build());
            current = current.plusMonths(1);
        }

        return MonthlyTrendResponse.builder()
                .fromMonth(startYM.toString())
                .toMonth(endYM.toString())
                .trends(trendItems)
                .build();
    }

    private MonthlyAnalyticsResponse calculateMonthlyAnalytics(UUID userId, YearMonth ym) {
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        BigDecimal income = transactionRepository.calculateTotalByUserIdAndTypeAndDateRange(
                userId, TransactionType.INCOME, startDate, endDate
        );
        if (income == null) income = BigDecimal.ZERO;
        income = income.setScale(2, RoundingMode.HALF_UP);

        BigDecimal expense = transactionRepository.calculateTotalByUserIdAndTypeAndDateRange(
                userId, TransactionType.EXPENSE, startDate, endDate
        );
        if (expense == null) expense = BigDecimal.ZERO;
        expense = expense.setScale(2, RoundingMode.HALF_UP);

        BigDecimal netSavings = income.subtract(expense).setScale(2, RoundingMode.HALF_UP);

        BigDecimal savingsRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings.multiply(BigDecimal.valueOf(100))
                    .divide(income, 2, RoundingMode.HALF_UP);
        }

        return MonthlyAnalyticsResponse.builder()
                .month(ym.toString())
                .totalIncome(income)
                .totalExpense(expense)
                .netSavings(netSavings)
                .savingsRate(savingsRate)
                .build();
    }

    private YearMonth parseYearMonthOrDefault(String monthStr) {
        if (monthStr == null || monthStr.trim().isEmpty()) {
            return YearMonth.now();
        }
        return parseYearMonth(monthStr);
    }

    private YearMonth parseYearMonth(String monthStr) {
        try {
            return YearMonth.parse(monthStr.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid month format. Expected YYYY-MM: " + monthStr);
        }
    }
}
