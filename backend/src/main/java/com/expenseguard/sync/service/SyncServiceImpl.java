package com.expenseguard.sync.service;

import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.security.CurrentUserService;
import com.expenseguard.common.exception.ResourceAccessDeniedException;
import com.expenseguard.common.exception.ResourceNotFoundException;
import com.expenseguard.sync.dto.*;
import com.expenseguard.sync.entity.*;
import com.expenseguard.sync.repository.SyncOperationRepository;
import com.expenseguard.transaction.entity.Account;
import com.expenseguard.transaction.entity.Category;
import com.expenseguard.transaction.entity.CategoryType;
import com.expenseguard.transaction.entity.Transaction;
import com.expenseguard.transaction.entity.TransactionType;
import com.expenseguard.transaction.repository.AccountRepository;
import com.expenseguard.transaction.repository.CategoryRepository;
import com.expenseguard.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation managing offline synchronization lifecycle, idempotency deduplication,
 * SHA-256 payload hashing, optimistic locking conflict detection, and atomic balance updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {

    private final SyncOperationRepository syncOperationRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public SyncTransactionResponse processSyncTransaction(SyncTransactionRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        return processSingleSyncOperation(request, currentUser);
    }

    @Override
    @Transactional
    public BatchSyncResponse processBatchSyncTransactions(BatchSyncRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        List<SyncTransactionResponse> results = new ArrayList<>();

        if (request.getOperations() != null) {
            for (SyncTransactionRequest opRequest : request.getOperations()) {
                try {
                    SyncTransactionResponse response = processSingleSyncOperation(opRequest, currentUser);
                    results.add(response);
                } catch (Exception ex) {
                    log.warn("Batch operation failed for clientOperationId {}: {}", opRequest.getClientOperationId(), ex.getMessage());
                    results.add(SyncTransactionResponse.builder()
                            .clientOperationId(opRequest.getClientOperationId())
                            .status(SyncOperationStatus.FAILED)
                            .transactionId(opRequest.getTransactionId())
                            .errorCode(ex.getClass().getSimpleName())
                            .message(ex.getMessage())
                            .build());
                }
            }
        }

        return BatchSyncResponse.builder().results(results).build();
    }

    @Override
    @Transactional(readOnly = true)
    public SyncStatusResponse getSyncStatus(String clientOperationId) {
        if (clientOperationId == null || clientOperationId.isBlank()) {
            throw new IllegalArgumentException("clientOperationId is required");
        }
        User currentUser = currentUserService.getCurrentUser();
        SyncOperation op = syncOperationRepository.findByUserIdAndClientOperationId(currentUser.getId(), clientOperationId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Sync operation not found for clientOperationId: " + clientOperationId));

        return SyncStatusResponse.builder()
                .clientOperationId(op.getClientOperationId())
                .status(op.getStatus())
                .transactionId(op.getEntityId())
                .errorCode(op.getErrorCode())
                .errorMessage(op.getErrorMessage())
                .createdAt(op.getCreatedAt())
                .processedAt(op.getProcessedAt())
                .build();
    }

    private SyncTransactionResponse processSingleSyncOperation(SyncTransactionRequest request, User currentUser) {
        validateBasicRequest(request);

        String clientOpId = request.getClientOperationId().trim();
        String currentPayloadHash = calculatePayloadHash(request);

        // Check for existing sync operation
        Optional<SyncOperation> existingSyncOpt = syncOperationRepository.findByUserIdAndClientOperationId(currentUser.getId(), clientOpId);
        if (existingSyncOpt.isPresent()) {
            SyncOperation existingSync = existingSyncOpt.get();

            // 1. Check Payload Mismatch
            if (existingSync.getPayloadHash() != null && !existingSync.getPayloadHash().equals(currentPayloadHash)) {
                log.warn("Payload hash mismatch for clientOperationId: {}", clientOpId);
                return SyncTransactionResponse.builder()
                        .clientOperationId(clientOpId)
                        .status(SyncOperationStatus.CONFLICT)
                        .transactionId(existingSync.getEntityId())
                        .errorCode("SYNC_OPERATION_PAYLOAD_MISMATCH")
                        .message("The clientOperationId was already processed with a different payload")
                        .build();
            }

            // 2. Return existing result (Idempotent retry)
            Long serverVersion = getTransactionServerVersion(existingSync.getEntityId());
            return SyncTransactionResponse.builder()
                    .clientOperationId(clientOpId)
                    .status(existingSync.getStatus())
                    .transactionId(existingSync.getEntityId())
                    .serverVersion(serverVersion)
                    .errorCode(existingSync.getErrorCode())
                    .message(existingSync.getStatus() == SyncOperationStatus.PROCESSED ?
                            "Transaction synchronized successfully" : existingSync.getErrorMessage())
                    .build();
        }

        // Handle Operation Types
        if (request.getOperationType() == SyncOperationType.CREATE) {
            return handleCreateSync(request, currentUser, currentPayloadHash);
        } else if (request.getOperationType() == SyncOperationType.UPDATE) {
            return handleUpdateSync(request, currentUser, currentPayloadHash);
        } else if (request.getOperationType() == SyncOperationType.DELETE) {
            return handleDeleteSync(request, currentUser, currentPayloadHash);
        } else {
            throw new IllegalArgumentException("Unsupported operationType: " + request.getOperationType());
        }
    }

    private SyncTransactionResponse handleCreateSync(SyncTransactionRequest request, User currentUser, String payloadHash) {
        validateCreateFields(request);

        Account account = verifyAccountOwnership(request.getAccountId(), currentUser.getId());
        Category category = verifyCategoryOwnership(request.getCategoryId(), currentUser.getId());
        validateCategoryTypeCompatibility(request.getType(), category.getType());

        // Idempotency double check on transaction level
        Optional<Transaction> existingTx = transactionRepository.findByUserIdAndClientOperationId(
                currentUser.getId(), request.getClientOperationId().trim()
        );

        Transaction transaction;
        if (existingTx.isPresent()) {
            transaction = existingTx.get();
        } else {
            transaction = Transaction.builder()
                    .user(currentUser)
                    .account(account)
                    .category(category)
                    .type(request.getType())
                    .amount(request.getAmount())
                    .transactionDate(request.getTransactionDate())
                    .description(request.getDescription())
                    .clientOperationId(request.getClientOperationId().trim())
                    .version(0L)
                    .build();

            transaction = transactionRepository.save(transaction);
            updateAccountBalance(account, request.getType(), request.getAmount());
        }

        recordSyncOperation(currentUser, request.getClientOperationId().trim(), SyncOperationType.CREATE,
                transaction.getId(), SyncOperationStatus.PROCESSED, payloadHash, null, null);

        return SyncTransactionResponse.builder()
                .clientOperationId(request.getClientOperationId().trim())
                .status(SyncOperationStatus.PROCESSED)
                .transactionId(transaction.getId())
                .serverVersion(transaction.getVersion())
                .message("Transaction synchronized successfully")
                .build();
    }

    private SyncTransactionResponse handleUpdateSync(SyncTransactionRequest request, User currentUser, String payloadHash) {
        validateUpdateFields(request);

        Transaction existingTx = findAndVerifyTransactionOwnership(request.getTransactionId(), currentUser.getId());

        // Optimistic Locking Version Check
        if (!request.getVersion().equals(existingTx.getVersion())) {
            log.warn("Version conflict on transaction {}: client version {}, server version {}",
                    existingTx.getId(), request.getVersion(), existingTx.getVersion());

            recordSyncOperation(currentUser, request.getClientOperationId().trim(), SyncOperationType.UPDATE,
                    existingTx.getId(), SyncOperationStatus.CONFLICT, payloadHash,
                    "TRANSACTION_CONFLICT", "The transaction was modified on the server.");

            return SyncTransactionResponse.builder()
                    .clientOperationId(request.getClientOperationId().trim())
                    .status(SyncOperationStatus.CONFLICT)
                    .transactionId(existingTx.getId())
                    .serverVersion(existingTx.getVersion())
                    .errorCode("TRANSACTION_CONFLICT")
                    .message("The transaction was modified on the server.")
                    .build();
        }

        Account newAccount = verifyAccountOwnership(request.getAccountId(), currentUser.getId());
        Category newCategory = verifyCategoryOwnership(request.getCategoryId(), currentUser.getId());
        validateCategoryTypeCompatibility(request.getType(), newCategory.getType());

        // Revert old balance impact & apply new balance impact
        Account oldAccount = existingTx.getAccount();
        revertAccountBalance(oldAccount, existingTx.getType(), existingTx.getAmount());

        if (oldAccount.getId().equals(newAccount.getId())) {
            updateAccountBalance(oldAccount, request.getType(), request.getAmount());
        } else {
            accountRepository.save(oldAccount);
            updateAccountBalance(newAccount, request.getType(), request.getAmount());
        }

        existingTx.setAccount(newAccount);
        existingTx.setCategory(newCategory);
        existingTx.setType(request.getType());
        existingTx.setAmount(request.getAmount());
        existingTx.setTransactionDate(request.getTransactionDate());
        existingTx.setDescription(request.getDescription());
        existingTx.setClientOperationId(request.getClientOperationId().trim());

        Transaction updatedTx = transactionRepository.save(existingTx);

        recordSyncOperation(currentUser, request.getClientOperationId().trim(), SyncOperationType.UPDATE,
                updatedTx.getId(), SyncOperationStatus.PROCESSED, payloadHash, null, null);

        return SyncTransactionResponse.builder()
                .clientOperationId(request.getClientOperationId().trim())
                .status(SyncOperationStatus.PROCESSED)
                .transactionId(updatedTx.getId())
                .serverVersion(updatedTx.getVersion())
                .message("Transaction synchronized successfully")
                .build();
    }

    private SyncTransactionResponse handleDeleteSync(SyncTransactionRequest request, User currentUser, String payloadHash) {
        validateDeleteFields(request);

        Optional<Transaction> txOpt = transactionRepository.findById(request.getTransactionId());
        if (txOpt.isEmpty()) {
            // Already deleted - idempotent success
            recordSyncOperation(currentUser, request.getClientOperationId().trim(), SyncOperationType.DELETE,
                    request.getTransactionId(), SyncOperationStatus.PROCESSED, payloadHash, null, null);

            return SyncTransactionResponse.builder()
                    .clientOperationId(request.getClientOperationId().trim())
                    .status(SyncOperationStatus.PROCESSED)
                    .transactionId(request.getTransactionId())
                    .message("Transaction deleted successfully")
                    .build();
        }

        Transaction existingTx = txOpt.get();
        if (!existingTx.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceAccessDeniedException("Access denied: You do not have permission to access this transaction");
        }

        revertAccountBalance(existingTx.getAccount(), existingTx.getType(), existingTx.getAmount());
        transactionRepository.delete(existingTx);

        recordSyncOperation(currentUser, request.getClientOperationId().trim(), SyncOperationType.DELETE,
                request.getTransactionId(), SyncOperationStatus.PROCESSED, payloadHash, null, null);

        return SyncTransactionResponse.builder()
                .clientOperationId(request.getClientOperationId().trim())
                .status(SyncOperationStatus.PROCESSED)
                .transactionId(request.getTransactionId())
                .message("Transaction deleted successfully")
                .build();
    }

    private void recordSyncOperation(User user, String clientOperationId, SyncOperationType opType,
                                      UUID entityId, SyncOperationStatus status, String payloadHash,
                                      String errorCode, String errorMessage) {
        SyncOperation syncOp = SyncOperation.builder()
                .user(user)
                .clientOperationId(clientOperationId)
                .operationType(opType)
                .entityType(SyncEntityType.TRANSACTION)
                .entityId(entityId)
                .status(status)
                .payloadHash(payloadHash)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .processedAt(Instant.now())
                .build();

        try {
            syncOperationRepository.save(syncOp);
        } catch (DataIntegrityViolationException ex) {
            log.info("Sync operation duplicate constraint caught for clientOperationId: {}", clientOperationId);
        }
    }

    private String calculatePayloadHash(SyncTransactionRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(request.getOperationType() != null ? request.getOperationType().name() : "").append("|");
            sb.append(request.getEntityType() != null ? request.getEntityType().name() : "TRANSACTION").append("|");
            sb.append(request.getTransactionId() != null ? request.getTransactionId().toString() : "").append("|");
            sb.append(request.getAccountId() != null ? request.getAccountId().toString() : "").append("|");
            sb.append(request.getCategoryId() != null ? request.getCategoryId().toString() : "").append("|");
            sb.append(request.getType() != null ? request.getType().name() : "").append("|");
            sb.append(request.getAmount() != null ? request.getAmount().stripTrailingZeros().toPlainString() : "").append("|");
            sb.append(request.getTransactionDate() != null ? request.getTransactionDate().toString() : "").append("|");
            sb.append(request.getDescription() != null ? request.getDescription().trim() : "").append("|");
            sb.append(request.getVersion() != null ? request.getVersion().toString() : "");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    private void validateBasicRequest(SyncTransactionRequest request) {
        if (request.getClientOperationId() == null || request.getClientOperationId().isBlank()) {
            throw new IllegalArgumentException("clientOperationId is required");
        }
        if (request.getClientOperationId().trim().length() > 100) {
            throw new IllegalArgumentException("clientOperationId must not exceed 100 characters");
        }
        if (request.getOperationType() == null) {
            throw new IllegalArgumentException("operationType is required");
        }
        if (request.getEntityType() != null && request.getEntityType() != SyncEntityType.TRANSACTION) {
            throw new IllegalArgumentException("Invalid entityType: Only TRANSACTION is supported");
        }
    }

    private void validateCreateFields(SyncTransactionRequest request) {
        if (request.getAccountId() == null) {
            throw new IllegalArgumentException("accountId is required for CREATE operation");
        }
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("categoryId is required for CREATE operation");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("type is required for CREATE operation");
        }
        validateAmount(request.getAmount());
        if (request.getTransactionDate() == null) {
            throw new IllegalArgumentException("transactionDate is required for CREATE operation");
        }
    }

    private void validateUpdateFields(SyncTransactionRequest request) {
        if (request.getTransactionId() == null) {
            throw new IllegalArgumentException("transactionId is required for UPDATE operation");
        }
        if (request.getVersion() == null) {
            throw new IllegalArgumentException("version is required for UPDATE operation");
        }
        if (request.getAccountId() == null) {
            throw new IllegalArgumentException("accountId is required for UPDATE operation");
        }
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("categoryId is required for UPDATE operation");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("type is required for UPDATE operation");
        }
        validateAmount(request.getAmount());
        if (request.getTransactionDate() == null) {
            throw new IllegalArgumentException("transactionDate is required for UPDATE operation");
        }
    }

    private void validateDeleteFields(SyncTransactionRequest request) {
        if (request.getTransactionId() == null) {
            throw new IllegalArgumentException("transactionId is required for DELETE operation");
        }
    }

    private Transaction findAndVerifyTransactionOwnership(UUID transactionId, UUID currentUserId) {
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

    private Long getTransactionServerVersion(UUID entityId) {
        if (entityId == null) return null;
        return transactionRepository.findById(entityId)
                .map(Transaction::getVersion)
                .orElse(null);
    }
}
