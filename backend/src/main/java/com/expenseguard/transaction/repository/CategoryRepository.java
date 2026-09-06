package com.expenseguard.transaction.repository;

import com.expenseguard.transaction.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Category entities.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Find all categories associated with a given user ID.
     */
    List<Category> findByUserId(UUID userId);

    /**
     * Find specific category owned by a given user ID.
     */
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
}
