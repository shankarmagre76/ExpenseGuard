package com.expenseguard.transaction.service;

import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.security.CurrentUserService;
import com.expenseguard.common.exception.ResourceAccessDeniedException;
import com.expenseguard.common.exception.ResourceNotFoundException;
import com.expenseguard.transaction.dto.TransactionRequest;
import com.expenseguard.transaction.dto.TransactionResponse;
import com.expenseguard.transaction.entity.Account;
import com.expenseguard.transaction.entity.Category;
import com.expenseguard.transaction.entity.CategoryType;
import com.expenseguard.transaction.entity.Transaction;
import com.expenseguard.transaction.entity.TransactionType;
import com.expenseguard.transaction.repository.AccountRepository;
import com.expenseguard.transaction.repository.CategoryRepository;
import com.expenseguard.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation enforcing server-side ownership, type compatibility,
 * idempotency deduplication, and atomic account balance updates for Transaction operations.
 */
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        // 1. Idempotency Check
        if (request.getClientOperationId() != null && !request.getClientOperationId().isBlank()) {
            Optional<Transaction> existingOpt = transactionRepository.findByUserIdAndClientOperationId(
                    currentUser.getId(), request.getClientOperationId().trim()
            );
            if (existingOpt.isPresent()) {
                return mapToResponse(existingOpt.get());
            }
        }

        // 2. Validate Amount
        validateAmount(request.getAmount());

        // 3. Verify Account Ownership
        Account account = verifyAccountOwnership(request.getAccountId(), currentUser.getId());

        // 4. Verify Category Ownership
        Category category = verifyCategoryOwnership(request.getCategoryId(), currentUser.getId());

        // 5. Verify Type Compatibility
        validateCategoryTypeCompatibility(request.getType(), category.getType());

        // 6. Build and Save Transaction
        Transaction transaction = Transaction.builder()
                .user(currentUser)
                .account(account)
                .category(category)
                .type(request.getType())
                .amount(request.getAmount())
                .transactionDate(request.getTransactionDate())
                .description(request.getDescription())
                .clientOperationId(request.getClientOperationId() != null ? request.getClientOperationId().trim() : null)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 7. Update Account Balance Atomically
        updateAccountBalance(account, request.getType(), request.getAmount());

        return mapToResponse(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactionsForCurrentUser(
            TransactionType type, UUID accountId, UUID categoryId, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        Page<Transaction> pageResult = transactionRepository.findFilteredTransactions(
                currentUserId, type, accountId, categoryId, fromDate, toDate, pageable
        );
        return pageResult.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactionsForCurrentUser(Pageable pageable) {
        return getAllTransactionsForCurrentUser(null, null, null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactionsForCurrentUser() {
        UUID currentUserId = currentUserService.getCurrentUserId();
        List<Transaction> transactions = transactionRepository.findAllByUserId(currentUserId);
        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID transactionId) {
        Transaction transaction = findAndVerifyOwnership(transactionId);
        return mapToResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(UUID transactionId, TransactionRequest request) {
        Transaction existing = findAndVerifyOwnership(transactionId);
        UUID currentUserId = currentUserService.getCurrentUserId();

        validateAmount(request.getAmount());

        Account newAccount = verifyAccountOwnership(request.getAccountId(), currentUserId);
        Category newCategory = verifyCategoryOwnership(request.getCategoryId(), currentUserId);

        validateCategoryTypeCompatibility(request.getType(), newCategory.getType());

        // 1. Revert previous transaction balance impact on old account
        Account oldAccount = existing.getAccount();
        revertAccountBalance(oldAccount, existing.getType(), existing.getAmount());

        if (oldAccount.getId().equals(newAccount.getId())) {
            // Same account: apply new transaction impact directly
            updateAccountBalance(oldAccount, request.getType(), request.getAmount());
        } else {
            // Different account: save oldAccount, apply new impact to newAccount
            accountRepository.save(oldAccount);
            updateAccountBalance(newAccount, request.getType(), request.getAmount());
        }

        // 2. Update transaction fields
        existing.setAccount(newAccount);
        existing.setCategory(newCategory);
        existing.setType(request.getType());
        existing.setAmount(request.getAmount());
        existing.setTransactionDate(request.getTransactionDate());
        existing.setDescription(request.getDescription());
        if (request.getClientOperationId() != null && !request.getClientOperationId().isBlank()) {
            existing.setClientOperationId(request.getClientOperationId().trim());
        }

        Transaction updated = transactionRepository.save(existing);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteTransaction(UUID transactionId) {
        Transaction existing = findAndVerifyOwnership(transactionId);

        Account account = existing.getAccount();
        revertAccountBalance(account, existing.getType(), existing.getAmount());

        transactionRepository.delete(existing);
    }

    private Transaction findAndVerifyOwnership(UUID transactionId) {
        UUID currentUserId = currentUserService.getCurrentUserId();

        return transactionRepository.findByIdAndUserId(transactionId, currentUserId)
                .orElseGet(() -> {
                    if (transactionRepository.existsById(transactionId)) {
                        throw new ResourceAccessDeniedException(
                                "Access denied: You do not have permission to access this transaction"
                        );
                    }
                    throw new ResourceNotFoundException("Transaction not found with ID: " + transactionId);
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
            throw new IllegalArgumentException("Transaction amount must be greater than zero");
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

    private void updateAccountBalance(Account account, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().subtract(amount));
        } else if (type == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount));
        }
        accountRepository.save(account);
    }

    private void revertAccountBalance(Account account, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().add(amount));
        } else if (type == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(amount));
        }
        accountRepository.save(account);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccount().getId())
                .categoryId(transaction.getCategory().getId())
                .categoryName(transaction.getCategory().getName())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .description(transaction.getDescription())
                .clientOperationId(transaction.getClientOperationId())
                .version(transaction.getVersion())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
