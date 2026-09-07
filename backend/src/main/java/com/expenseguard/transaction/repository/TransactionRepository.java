package com.expenseguard.transaction.repository;

import com.expenseguard.transaction.entity.Transaction;
import com.expenseguard.transaction.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Transaction entities with user-ownership querying capabilities.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Find all transactions owned by a given user ID with pagination support.
     */
    Page<Transaction> findAllByUserId(UUID userId, Pageable pageable);

    /**
     * Calculates total spent amount for a specific user, category, and date range for EXPENSE type.
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.category.id = :categoryId
          AND t.type = com.expenseguard.transaction.entity.TransactionType.EXPENSE
          AND t.transactionDate >= :startDate
          AND t.transactionDate <= :endDate
    """)
    BigDecimal calculateSpentAmount(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find all transactions owned by a given user ID with optional filters.
     */
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.id = :userId
          AND (:type IS NULL OR t.type = :type)
          AND (:accountId IS NULL OR t.account.id = :accountId)
          AND (:categoryId IS NULL OR t.category.id = :categoryId)
          AND (cast(:fromDate as date) IS NULL OR t.transactionDate >= :fromDate)
          AND (cast(:toDate as date) IS NULL OR t.transactionDate <= :toDate)
    """)
    Page<Transaction> findFilteredTransactions(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    /**
     * Find all transactions owned by a given user ID.
     */
    List<Transaction> findByUserId(UUID userId);

    /**
     * Find all transactions owned by a given user ID (alias for strict ownership naming).
     */
    List<Transaction> findAllByUserId(UUID userId);

    /**
     * Find specific transaction owned by a given user ID.
     */
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find transaction by user ID and client operation ID for idempotency deduplication.
     */
    Optional<Transaction> findByUserIdAndClientOperationId(UUID userId, String clientOperationId);

    /**
     * Find all transactions associated with a specific account ID.
     */
    List<Transaction> findByAccountId(UUID accountId);

    /**
     * Find all transactions categorized under a specific category ID.
     */
    List<Transaction> findByCategoryId(UUID categoryId);

    /**
     * Calculates total transaction amount for a user, type, and date range.
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = :type
          AND t.transactionDate >= :startDate
          AND t.transactionDate <= :endDate
    """)
    BigDecimal calculateTotalByUserIdAndTypeAndDateRange(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Aggregate expense transactions grouped by category for a user and date range.
     */
    @Query("""
        SELECT t.category.id AS categoryId,
               t.category.name AS categoryName,
               SUM(t.amount) AS amount
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = com.expenseguard.transaction.entity.TransactionType.EXPENSE
          AND t.transactionDate >= :startDate
          AND t.transactionDate <= :endDate
        GROUP BY t.category.id, t.category.name
        ORDER BY SUM(t.amount) DESC
    """)
    List<com.expenseguard.analytics.projection.CategoryExpenseProjection> findCategoryExpensesByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Aggregate total income and expense grouped by account for a user and date range.
     */
    @Query("""
        SELECT t.account.id AS accountId,
               t.account.name AS accountName,
               SUM(CASE WHEN t.type = com.expenseguard.transaction.entity.TransactionType.INCOME THEN t.amount ELSE 0 END) AS totalIncome,
               SUM(CASE WHEN t.type = com.expenseguard.transaction.entity.TransactionType.EXPENSE THEN t.amount ELSE 0 END) AS totalExpense
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.transactionDate >= :startDate
          AND t.transactionDate <= :endDate
        GROUP BY t.account.id, t.account.name
    """)
    List<com.expenseguard.analytics.projection.AccountSummaryProjection> findAccountSummariesByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
