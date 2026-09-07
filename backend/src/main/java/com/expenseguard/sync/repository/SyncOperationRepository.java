package com.expenseguard.sync.repository;

import com.expenseguard.sync.entity.SyncOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for managing SyncOperation entities.
 */
@Repository
public interface SyncOperationRepository extends JpaRepository<SyncOperation, UUID> {

    Optional<SyncOperation> findByUserIdAndClientOperationId(UUID userId, String clientOperationId);

    Optional<SyncOperation> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndClientOperationId(UUID userId, String clientOperationId);
}
