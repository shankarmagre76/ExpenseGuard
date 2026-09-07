package com.expenseguard.budget.repository;

import com.expenseguard.budget.entity.Budget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Budget entities with user-ownership querying capabilities.
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    /**
     * Find all budgets owned by a given user ID with pagination support.
     */
    Page<Budget> findAllByUserId(UUID userId, Pageable pageable);

    /**
     * Find all budgets owned by a given user ID.
     */
    List<Budget> findAllByUserId(UUID userId);

    /**
     * Find all budgets owned by a given user ID for a specific month.
     */
    List<Budget> findByUserIdAndMonth(UUID userId, String month);

    /**
     * Find specific budget owned by a given user ID.
     */
    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find specific budget for user, category, and month.
     */
    Optional<Budget> findByUserIdAndCategoryIdAndMonth(UUID userId, UUID categoryId, String month);

    /**
     * Checks if a budget exists for a specific user, category, and month.
     */
    boolean existsByUserIdAndCategoryIdAndMonth(UUID userId, UUID categoryId, String month);

    /**
     * Checks if another budget exists for a specific user, category, and month excluding target ID.
     */
    boolean existsByUserIdAndCategoryIdAndMonthAndIdNot(UUID userId, UUID categoryId, String month, UUID id);
}
