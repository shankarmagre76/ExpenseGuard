package com.expenseguard.transaction.service;

import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.security.CurrentUserService;
import com.expenseguard.common.exception.CategoryAlreadyExistsException;
import com.expenseguard.common.exception.ResourceAccessDeniedException;
import com.expenseguard.common.exception.ResourceNotFoundException;
import com.expenseguard.transaction.dto.CategoryRequest;
import com.expenseguard.transaction.dto.CategoryResponse;
import com.expenseguard.transaction.entity.Category;
import com.expenseguard.transaction.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation enforcing server-side ownership and uniqueness for Category operations.
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        String name = request.getName().trim();

        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(currentUser.getId(), name, request.getType())) {
            throw new CategoryAlreadyExistsException(
                    "Category with name '" + name + "' and type '" + request.getType() + "' already exists"
            );
        }

        Category category = Category.builder()
                .user(currentUser)
                .name(name)
                .type(request.getType())
                .build();

        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesForCurrentUser() {
        UUID currentUserId = currentUserService.getCurrentUserId();
        List<Category> categories = categoryRepository.findAllByUserId(currentUserId);
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID categoryId) {
        UUID currentUserId = currentUserService.getCurrentUserId();

        Category category = categoryRepository.findByIdAndUserId(categoryId, currentUserId)
                .orElseGet(() -> {
                    if (categoryRepository.existsById(categoryId)) {
                        throw new ResourceAccessDeniedException(
                                "Access denied: You do not have permission to access this category"
                        );
                    }
                    throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
                });

        return mapToResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, CategoryRequest request) {
        Category category = findAndVerifyOwnership(categoryId);
        String name = request.getName().trim();

        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(
                category.getUser().getId(), name, request.getType(), categoryId)) {
            throw new CategoryAlreadyExistsException(
                    "Category with name '" + name + "' and type '" + request.getType() + "' already exists"
            );
        }

        category.setName(name);
        category.setType(request.getType());

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID categoryId) {
        Category category = findAndVerifyOwnership(categoryId);
        categoryRepository.delete(category);
    }

    private Category findAndVerifyOwnership(UUID categoryId) {
        UUID currentUserId = currentUserService.getCurrentUserId();

        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
        }

        Category category = categoryOpt.get();

        if (!category.getUser().getId().equals(currentUserId)) {
            throw new ResourceAccessDeniedException(
                    "Access denied: You do not have permission to access this category"
            );
        }

        return category;
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
