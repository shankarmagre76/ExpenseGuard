package com.expenseguard.transaction.service;

import com.expenseguard.transaction.dto.CategoryRequest;
import com.expenseguard.transaction.dto.CategoryResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for managing user categories with strict server-side ownership enforcement.
 */
public interface CategoryService {

    /**
     * Creates a new transaction category for the currently authenticated user.
     *
     * @param request Category creation payload
     * @return Created CategoryResponse DTO
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Retrieves all categories belonging to the currently authenticated user.
     *
     * @return List of CategoryResponse DTOs
     */
    List<CategoryResponse> getAllCategoriesForCurrentUser();

    /**
     * Retrieves a category by ID ensuring ownership by the currently authenticated user.
     *
     * @param categoryId Category UUID
     * @return CategoryResponse DTO
     */
    CategoryResponse getCategoryById(UUID categoryId);

    /**
     * Updates an existing category owned by the currently authenticated user.
     *
     * @param categoryId Category UUID to update
     * @param request Category update payload
     * @return Updated CategoryResponse DTO
     */
    CategoryResponse updateCategory(UUID categoryId, CategoryRequest request);

    /**
     * Deletes a category owned by the currently authenticated user.
     *
     * @param categoryId Category UUID to delete
     */
    void deleteCategory(UUID categoryId);
}
