package com.expenseguard.receipt.repository;

import com.expenseguard.receipt.entity.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for managing Receipt entity persistence.
 */
@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    /**
     * Find all receipts owned by a specific user with pagination support.
     */
    Page<Receipt> findAllByUserId(UUID userId, Pageable pageable);

    /**
     * Find specific receipt owned by user ID.
     */
    Optional<Receipt> findByIdAndUserId(UUID id, UUID userId);
}
