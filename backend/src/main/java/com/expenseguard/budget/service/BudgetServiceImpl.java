package com.expenseguard.budget.service;

import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.security.CurrentUserService;
import com.expenseguard.budget.dto.BudgetRequest;
import com.expenseguard.budget.dto.BudgetResponse;
import com.expenseguard.budget.entity.Budget;
import com.expenseguard.budget.repository.BudgetRepository;
import com.expenseguard.common.exception.BudgetAlreadyExistsException;
import com.expenseguard.common.exception.ResourceAccessDeniedException;
import com.expenseguard.common.exception.ResourceNotFoundException;
import com.expenseguard.transaction.entity.Category;
import com.expenseguard.transaction.entity.CategoryType;
import com.expenseguard.transaction.repository.CategoryRepository;
import com.expenseguard.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation enforcing server-side ownership, EXPENSE category rules,
 * uniqueness per month, and real-time budget utilization calculations.
 */
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        // 1. Validate Month
        String month = parseAndValidateMonth(request.getMonth());

        // 2. Validate Amount
        validateAmount(request.getAmount());

        // 3. Verify Category Ownership & EXPENSE type
        Category category = verifyCategoryOwnershipAndType(request.getCategoryId(), currentUser.getId());

        // 4. Duplicate Check per User, Category, and Month
        if (budgetRepository.existsByUserIdAndCategoryIdAndMonth(currentUser.getId(), category.getId(), month)) {
            throw new BudgetAlreadyExistsException(
                    "Budget already exists for category '" + category.getName() + "' and month '" + month + "'"
            );
        }

        // 5. Build and Save Budget
        Budget budget = Budget.builder()
                .user(currentUser)
                .category(category)
                .amount(request.getAmount())
                .month(month)
                .build();

        Budget saved = budgetRepository.save(budget);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudgetsForCurrentUser(String monthFilter) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        List<Budget> budgets;

        if (monthFilter != null && !monthFilter.isBlank()) {
            String month = parseAndValidateMonth(monthFilter);
            budgets = budgetRepository.findByUserIdAndMonth(currentUserId, month);
        } else {
            budgets = budgetRepository.findAllByUserId(currentUserId);
        }

        return budgets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(UUID budgetId) {
        Budget budget = findAndVerifyOwnership(budgetId);
        return mapToResponse(budget);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetSummary(UUID budgetId) {
        return getBudgetById(budgetId);
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(UUID budgetId, BudgetRequest request) {
        Budget existing = findAndVerifyOwnership(budgetId);
        UUID currentUserId = currentUserService.getCurrentUserId();

        String month = parseAndValidateMonth(request.getMonth());
        validateAmount(request.getAmount());

        Category newCategory = verifyCategoryOwnershipAndType(request.getCategoryId(), currentUserId);

        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndIdNot(
                currentUserId, newCategory.getId(), month, budgetId)) {
            throw new BudgetAlreadyExistsException(
                    "Budget already exists for category '" + newCategory.getName() + "' and month '" + month + "'"
            );
        }

        existing.setCategory(newCategory);
        existing.setAmount(request.getAmount());
        existing.setMonth(month);

        Budget updated = budgetRepository.save(existing);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBudget(UUID budgetId) {
        Budget existing = findAndVerifyOwnership(budgetId);
        budgetRepository.delete(existing);
    }

    private Budget findAndVerifyOwnership(UUID budgetId) {
        UUID currentUserId = currentUserService.getCurrentUserId();

        return budgetRepository.findByIdAndUserId(budgetId, currentUserId)
                .orElseGet(() -> {
                    if (budgetRepository.existsById(budgetId)) {
                        throw new ResourceAccessDeniedException(
                                "Access denied: You do not have permission to access this budget"
                        );
                    }
                    throw new ResourceNotFoundException("Budget not found with ID: " + budgetId);
                });
    }

    private Category verifyCategoryOwnershipAndType(UUID categoryId, UUID currentUserId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, currentUserId)
                .orElseGet(() -> {
                    if (categoryRepository.existsById(categoryId)) {
                        throw new ResourceAccessDeniedException(
                                "Access denied: Category does not belong to authenticated user"
                        );
                    }
                    throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
                });

        if (category.getType() != CategoryType.EXPENSE) {
            throw new IllegalArgumentException("Budgets can only be created for EXPENSE categories");
        }

        return category;
    }

    private String parseAndValidateMonth(String monthStr) {
        if (monthStr == null || monthStr.isBlank()) {
            throw new IllegalArgumentException("Month is required");
        }
        String trimmed = monthStr.trim();
        try {
            YearMonth.parse(trimmed);
            return trimmed;
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid month format. Expected YYYY-MM (e.g., 2026-09)");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Budget amount must be greater than zero");
        }
    }

    private BudgetResponse mapToResponse(Budget budget) {
        String month = budget.getMonth();
        LocalDate startDate = YearMonth.parse(month).atDay(1);
        LocalDate endDate = YearMonth.parse(month).atEndOfMonth();

        BigDecimal spentAmount = transactionRepository.calculateSpentAmount(
                budget.getUser().getId(),
                budget.getCategory().getId(),
                startDate,
                endDate
        );

        if (spentAmount == null) {
            spentAmount = BigDecimal.ZERO;
        }

        BigDecimal targetAmount = budget.getAmount();
        BigDecimal remainingAmount = targetAmount.subtract(spentAmount);

        BigDecimal utilizationPercentage = BigDecimal.ZERO;
        if (targetAmount.compareTo(BigDecimal.ZERO) > 0) {
            utilizationPercentage = spentAmount.multiply(BigDecimal.valueOf(100))
                    .divide(targetAmount, 2, RoundingMode.HALF_UP);
        }

        return BudgetResponse.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .month(month)
                .amount(targetAmount)
                .budgetAmount(targetAmount)
                .spentAmount(spentAmount)
                .remainingAmount(remainingAmount)
                .utilizationPercentage(utilizationPercentage)
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }
}
