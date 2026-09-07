package com.expenseguard.budget.controller;

import com.expenseguard.budget.dto.BudgetRequest;
import com.expenseguard.budget.dto.BudgetResponse;
import com.expenseguard.budget.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Protected REST Controller mapping /api/v1/budgets endpoints for Budget management.
 */
@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * Creates a new budget allocation for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.createBudget(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all budgets belonging to the authenticated user with optional month filter.
     */
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getAllBudgets(
            @RequestParam(name = "month", required = false) String month
    ) {
        List<BudgetResponse> response = budgetService.getAllBudgetsForCurrentUser(month);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific budget by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudgetById(@PathVariable("id") UUID id) {
        BudgetResponse response = budgetService.getBudgetById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves budget summary with utilization math by ID.
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<BudgetResponse> getBudgetSummary(@PathVariable("id") UUID id) {
        BudgetResponse response = budgetService.getBudgetSummary(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing budget owned by the authenticated user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable("id") UUID id,
            @Valid @RequestBody BudgetRequest request
    ) {
        BudgetResponse response = budgetService.updateBudget(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a budget owned by the authenticated user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable("id") UUID id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
