package com.expenseguard.recurring.service;

import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.security.CurrentUserService;
import com.expenseguard.common.exception.ResourceAccessDeniedException;
import com.expenseguard.common.exception.ResourceNotFoundException;
import com.expenseguard.recurring.dto.RecurringTransactionRequest;
import com.expenseguard.recurring.dto.RecurringTransactionResponse;
import com.expenseguard.recurring.dto.RecurringTransactionStatusRequest;
import com.expenseguard.recurring.entity.RecurringTransaction;
import com.expenseguard.recurring.repository.RecurringTransactionRepository;
import com.expenseguard.transaction.entity.Account;
import com.expenseguard.transaction.entity.Category;
import com.expenseguard.transaction.entity.CategoryType;
import com.expenseguard.transaction.entity.TransactionType;
import com.expenseguard.transaction.repository.AccountRepository;
import com.expenseguard.transaction.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation managing recurring transaction templates,
 * enforcing ownership security, type compatibility, and status toggling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public RecurringTransactionResponse createRecurringTransaction(RecurringTransactionRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        // 1. Validate Amount
        validateAmount(request.getAmount());

        // 2. Validate Dates
        validateDates(request.getStartDate(), request.getNextRunDate(), request.getEndDate());

        // 3. Verify Account Ownership
        Account account = verifyAccountOwnership(request.getAccountId(), currentUser.getId());

        // 4. Verify Category Ownership
        Category category = verifyCategoryOwnership(request.getCategoryId(), currentUser.getId());

        // 5. Verify Type Compatibility
        validateCategoryTypeCompatibility(request.getType(), category.getType());

        // 6. Build and Save Recurring Transaction
        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .user(currentUser)
                .account(account)
                .category(category)
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .frequency(request.getFrequency())
                .startDate(request.getStartDate())
                .nextRunDate(request.getNextRunDate())
                .endDate(request.getEndDate())
                .active(true)
                .build();

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getAllRecurringTransactionsForCurrentUser() {
        UUID currentUserId = currentUserService.getCurrentUserId();
        List<RecurringTransaction> items = recurringTransactionRepository.findAllByUserId(currentUserId);
        return items.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringTransactionResponse getRecurringTransactionById(UUID id) {
        RecurringTransaction item = findAndVerifyOwnership(id);
        return mapToResponse(item);
    }

    @Override
    @Transactional
    public RecurringTransactionResponse updateRecurringTransaction(UUID id, RecurringTransactionRequest request) {
        RecurringTransaction existing = findAndVerifyOwnership(id);
        UUID currentUserId = currentUserService.getCurrentUserId();

        validateAmount(request.getAmount());
        validateDates(request.getStartDate(), request.getNextRunDate(), request.getEndDate());

        Account newAccount = verifyAccountOwnership(request.getAccountId(), currentUserId);
        Category newCategory = verifyCategoryOwnership(request.getCategoryId(), currentUserId);

        validateCategoryTypeCompatibility(request.getType(), newCategory.getType());

        existing.setAccount(newAccount);
        existing.setCategory(newCategory);
        existing.setType(request.getType());
        existing.setAmount(request.getAmount());
        existing.setDescription(request.getDescription());
        existing.setFrequency(request.getFrequency());
        existing.setStartDate(request.getStartDate());
        existing.setNextRunDate(request.getNextRunDate());
        existing.setEndDate(request.getEndDate());

        RecurringTransaction updated = recurringTransactionRepository.save(existing);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteRecurringTransaction(UUID id) {
        RecurringTransaction existing = findAndVerifyOwnership(id);
        recurringTransactionRepository.delete(existing);
    }

    @Override
    @Transactional
    public RecurringTransactionResponse updateStatus(UUID id, RecurringTransactionStatusRequest statusRequest) {
        RecurringTransaction existing = findAndVerifyOwnership(id);
        existing.setActive(statusRequest.getActive());
        RecurringTransaction updated = recurringTransactionRepository.save(existing);
        return mapToResponse(updated);
    }

    private RecurringTransaction findAndVerifyOwnership(UUID id) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        return recurringTransactionRepository.findByIdAndUserId(id, currentUserId)
                .orElseGet(() -> {
                    if (recurringTransactionRepository.existsById(id)) {
                        throw new ResourceAccessDeniedException(
                                "Access denied: You do not have permission to access this recurring transaction"
                        );
                    }
                    throw new ResourceNotFoundException("Recurring transaction not found with ID: " + id);
                });
    }

    private Account verifyAccountOwnership(UUID accountId, UUID currentUserId) {
        return accountRepository.findByIdAndUserId(accountId, currentUserId)
                .orElseGet(() -> {
                    if (accountRepository.existsById(accountId)) {
                        throw new ResourceAccessDeniedException(
                                "Access denied: Account does not belong to authenticated user"
                        );
                    }
                    throw new ResourceNotFoundException("Account not found with ID: " + accountId);
                });
    }

    private Category verifyCategoryOwnership(UUID categoryId, UUID currentUserId) {
        return categoryRepository.findByIdAndUserId(categoryId, currentUserId)
                .orElseGet(() -> {
                    if (categoryRepository.existsById(categoryId)) {
                        throw new ResourceAccessDeniedException(
                                "Access denied: Category does not belong to authenticated user"
                        );
                    }
                    throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
                });
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate nextRunDate, LocalDate endDate) {
        if (startDate == null || nextRunDate == null) {
            throw new IllegalArgumentException("Start date and next run date are required");
        }
        if (nextRunDate.isBefore(startDate)) {
            throw new IllegalArgumentException("nextRunDate cannot be before startDate");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate cannot be before startDate");
        }
    }

    private void validateCategoryTypeCompatibility(TransactionType transactionType, CategoryType categoryType) {
        if (transactionType == TransactionType.EXPENSE && categoryType != CategoryType.EXPENSE) {
            throw new IllegalArgumentException(
                    "Transaction type EXPENSE is incompatible with category type " + categoryType
            );
        }
        if (transactionType == TransactionType.INCOME && categoryType != CategoryType.INCOME) {
            throw new IllegalArgumentException(
                    "Transaction type INCOME is incompatible with category type " + categoryType
            );
        }
    }

    private RecurringTransactionResponse mapToResponse(RecurringTransaction item) {
        return RecurringTransactionResponse.builder()
                .id(item.getId())
                .accountId(item.getAccount().getId())
                .categoryId(item.getCategory().getId())
                .categoryName(item.getCategory().getName())
                .type(item.getType())
                .amount(item.getAmount())
                .description(item.getDescription())
                .frequency(item.getFrequency())
                .startDate(item.getStartDate())
                .nextRunDate(item.getNextRunDate())
                .endDate(item.getEndDate())
                .active(item.isActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
