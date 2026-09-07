package com.expenseguard.budget.service;

import com.expenseguard.budget.dto.BudgetRequest;
import com.expenseguard.budget.dto.BudgetResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for managing user budget resources with strict ownership enforcement,
 * EXPENSE category validation, and dynamic budget utilization calculation.
 */
public interface BudgetService {

    /**
     * Creates a new category budget for the currently authenticated user.
     *
     * @param request Budget creation payload
     * @return Created BudgetResponse DTO
     */
    BudgetResponse createBudget(BudgetRequest request);

    /**
     * Retrieves all budgets belonging to the currently authenticated user with optional month filter.
     *
     * @param month Optional month filter (format YYYY-MM)
     * @return List of BudgetResponse DTOs
     */
    List<BudgetResponse> getAllBudgetsForCurrentUser(String month);

    /**
     * Retrieves a budget by ID ensuring ownership by the currently authenticated user.
     *
     * @param budgetId Budget UUID
     * @return BudgetResponse DTO
     */
    BudgetResponse getBudgetById(UUID budgetId);

    /**
     * Retrieves budget summary with utilization math by ID for the currently authenticated user.
     *
     * @param budgetId Budget UUID
     * @return BudgetResponse DTO
     */
    BudgetResponse getBudgetSummary(UUID budgetId);

    /**
     * Updates an existing budget owned by the currently authenticated user.
     *
     * @param budgetId Budget UUID to update
     * @param request Budget update payload
     * @return Updated BudgetResponse DTO
     */
    BudgetResponse updateBudget(UUID budgetId, BudgetRequest request);

    /**
     * Deletes a budget owned by the currently authenticated user.
     *
     * @param budgetId Budget UUID to delete
     */
    void deleteBudget(UUID budgetId);
}
