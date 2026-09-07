package com.expenseguard.transaction;

import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.auth.service.AuthService;
import com.expenseguard.transaction.dto.CategoryRequest;
import com.expenseguard.transaction.entity.Category;
import com.expenseguard.transaction.entity.CategoryType;
import com.expenseguard.transaction.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test suite verifying Phase 3.2 Categories Management requirements.
 */
import com.expenseguard.ExpenseGuardApplication;

@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private Category userACategory;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register User A
        RegisterRequest registerA = RegisterRequest.builder()
                .name("User A")
                .email("testa@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(registerA);
        userA = userRepository.findByEmail("testa@example.com").orElseThrow();
        tokenA = jwtService.generateToken(userA);

        // 2. Register User B
        RegisterRequest registerB = RegisterRequest.builder()
                .name("User B")
                .email("testb@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(registerB);
        userB = userRepository.findByEmail("testb@example.com").orElseThrow();
        tokenB = jwtService.generateToken(userB);

        // 3. Create default category for User A
        userACategory = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Food")
                .type(CategoryType.EXPENSE)
                .build());
    }

    @Test
    @DisplayName("TEST 1: User A creates category (201 Created, persisted in DB)")
    void test1_CreateCategory_UserA_Returns201Created() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Shopping")
                .type(CategoryType.EXPENSE)
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Shopping")))
                .andExpect(jsonPath("$.type", is("EXPENSE")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        assertThat(categoryRepository.findByUserId(userA.getId())).hasSize(2);
    }

    @Test
    @DisplayName("TEST 2: User A gets categories (returns only User A categories)")
    void test2_GetMyCategories_UserA_ReturnsUserACategoriesOnly() throws Exception {
        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Food")))
                .andExpect(jsonPath("$[0].type", is("EXPENSE")));
    }

    @Test
    @DisplayName("TEST 3: User B creates category (201 Created)")
    void test3_UserB_CreatesCategory_Returns201Created() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Travel")
                .type(CategoryType.EXPENSE)
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Travel")))
                .andExpect(jsonPath("$.type", is("EXPENSE")));

        assertThat(categoryRepository.findByUserId(userB.getId())).hasSize(1);
    }

    @Test
    @DisplayName("TEST 4: User B lists categories (User B sees only User B categories, User A categories absent)")
    void test4_UserB_ListsCategories_DoesNotContainUserACategories() throws Exception {
        categoryRepository.save(Category.builder()
                .user(userB)
                .name("Travel")
                .type(CategoryType.EXPENSE)
                .build());

        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Travel")));
    }

    @Test
    @DisplayName("TEST 5: User A requests User B's category (403 Forbidden)")
    void test5_UserA_RequestsUserBCategory_Returns403Forbidden() throws Exception {
        Category categoryB = categoryRepository.save(Category.builder()
                .user(userB)
                .name("Freelance")
                .type(CategoryType.INCOME)
                .build());

        mockMvc.perform(get("/api/v1/categories/" + categoryB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("TEST 6: User B updates User A's category (403 Forbidden)")
    void test6_UserB_UpdatesUserACategory_Returns403Forbidden() throws Exception {
        CategoryRequest updateRequest = CategoryRequest.builder()
                .name("Hacked Category")
                .type(CategoryType.EXPENSE)
                .build();

        mockMvc.perform(put("/api/v1/categories/" + userACategory.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        Category categoryInDb = categoryRepository.findById(userACategory.getId()).orElseThrow();
        assertThat(categoryInDb.getName()).isEqualTo("Food");
    }

    @Test
    @DisplayName("TEST 7: User B deletes User A's category (403 Forbidden)")
    void test7_UserB_DeletesUserACategory_Returns403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/" + userACategory.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        assertThat(categoryRepository.findById(userACategory.getId())).isPresent();
    }

    @Test
    @DisplayName("TEST 8: User A updates own category (200 OK, verified in DB)")
    void test8_UserA_UpdatesOwnCategory_Returns200OK() throws Exception {
        CategoryRequest updateRequest = CategoryRequest.builder()
                .name("Groceries")
                .type(CategoryType.EXPENSE)
                .build();

        mockMvc.perform(put("/api/v1/categories/" + userACategory.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Groceries")))
                .andExpect(jsonPath("$.type", is("EXPENSE")));

        Optional<Category> updatedInDb = categoryRepository.findById(userACategory.getId());
        assertThat(updatedInDb).isPresent();
        assertThat(updatedInDb.get().getName()).isEqualTo("Groceries");
    }

    @Test
    @DisplayName("TEST 9: User A deletes own category (204 No Content, verified in DB)")
    void test9_UserA_DeletesOwnCategory_Returns204NoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/" + userACategory.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        assertThat(categoryRepository.findById(userACategory.getId())).isEmpty();
    }

    @Test
    @DisplayName("TEST 10: No JWT (Unauthenticated request returns 401 Unauthorized)")
    void test10_NoJwt_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TEST 11: Invalid category type (400 Bad Request)")
    void test11_InvalidCategoryType_Returns400BadRequest() throws Exception {
        String invalidJson = """
                {
                  "name": "Food",
                  "type": "INVALID"
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("TEST 12: Blank name (400 Bad Request)")
    void test12_BlankName_Returns400BadRequest() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("")
                .type(CategoryType.EXPENSE)
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("TEST 13: Duplicate category for same user and type (409 Conflict)")
    void test13_DuplicateCategory_Returns409Conflict() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Food")
                .type(CategoryType.EXPENSE)
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    @Test
    @DisplayName("TEST 14: Client-supplied user ID in payload is ignored; JWT identity determines ownership")
    void test14_ClientSuppliedUserId_IsIgnoredByServer() throws Exception {
        String spoofedPayload = String.format("""
                {
                  "name": "Travel",
                  "type": "EXPENSE",
                  "userId": "%s"
                }
                """, userB.getId());

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spoofedPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Travel")));

        // Verify category belongs to User A, NOT User B
        Category created = categoryRepository.findByUserId(userA.getId()).stream()
                .filter(c -> c.getName().equals("Travel"))
                .findFirst()
                .orElseThrow();
        assertThat(created.getUser().getId()).isEqualTo(userA.getId());
    }
}
