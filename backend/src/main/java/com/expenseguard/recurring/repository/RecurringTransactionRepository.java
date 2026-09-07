package com.expenseguard.recurring.repository;

import com.expenseguard.recurring.entity.RecurringTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing RecurringTransaction persistence and locking due queries.
 */
@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {

    /**
     * Find all recurring transactions belonging to a specific user.
     */
    List<RecurringTransaction> findAllByUserId(UUID userId);

    /**
     * Find specific recurring transaction owned by user ID.
     */
    Optional<RecurringTransaction> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find all active due recurring transactions with pessimistic write locking for concurrency protection.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM RecurringTransaction r
        WHERE r.active = true
          AND r.nextRunDate <= :today
          AND (r.endDate IS NULL OR r.nextRunDate <= r.endDate)
    """)
    List<RecurringTransaction> findDueRecurringTransactionsWithLock(@Param("today") LocalDate today);

    /**
     * Find specific due recurring transaction with pessimistic write locking.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM RecurringTransaction r
        WHERE r.id = :id
          AND r.active = true
          AND r.nextRunDate <= :today
          AND (r.endDate IS NULL OR r.nextRunDate <= r.endDate)
    """)
    Optional<RecurringTransaction> findDueByIdWithLock(@Param("id") UUID id, @Param("today") LocalDate today);
}
