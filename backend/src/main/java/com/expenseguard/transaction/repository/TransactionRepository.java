package com.expenseguard.transaction.repository;

import com.expenseguard.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Transaction entities.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Find all transactions owned by a given user ID.
     */
    List<Transaction> findByUserId(UUID userId);

    /**
     * Find specific transaction owned by a given user ID.
     */
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find all transactions associated with a specific account ID.
     */
    List<Transaction> findByAccountId(UUID accountId);

    /**
     * Find all transactions categorized under a specific category ID.
     */
    List<Transaction> findByCategoryId(UUID categoryId);
}
