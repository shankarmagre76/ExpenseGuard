package com.expenseguard.transaction.repository;

import com.expenseguard.transaction.entity.Category;
import com.expenseguard.transaction.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Category entities with user-ownership querying capabilities.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Find all categories associated with a given user ID.
     */
    List<Category> findByUserId(UUID userId);

    /**
     * Find all categories associated with a given user ID (alias for strict ownership naming).
     */
    List<Category> findAllByUserId(UUID userId);

    /**
     * Find specific category owned by a given user ID.
     */
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Checks if a category with the specified name and type exists for a specific user (case-insensitive name).
     */
    boolean existsByUserIdAndNameIgnoreCaseAndType(UUID userId, String name, CategoryType type);

    /**
     * Checks if another category with the specified name and type exists for a specific user excluding target ID (case-insensitive name).
     */
    boolean existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(UUID userId, String name, CategoryType type, UUID id);
}
