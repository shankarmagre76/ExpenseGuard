package com.expenseguard.transaction;

import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.auth.service.AuthService;
import com.expenseguard.transaction.dto.TransactionRequest;
import com.expenseguard.transaction.entity.Account;
import com.expenseguard.transaction.entity.AccountType;
import com.expenseguard.transaction.entity.Category;
import com.expenseguard.transaction.entity.CategoryType;
import com.expenseguard.transaction.entity.Transaction;
import com.expenseguard.transaction.entity.TransactionType;
import com.expenseguard.transaction.repository.AccountRepository;
import com.expenseguard.transaction.repository.CategoryRepository;
import com.expenseguard.transaction.repository.TransactionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test suite verifying Phase 3.3 Transactions Management requirements.
 */
import com.expenseguard.ExpenseGuardApplication;

@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

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
    private Account userAAccount;
    private Account userBAccount;
    private Category userAExpenseCat;
    private Category userAIncomeCat;
    private Category userBExpenseCat;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
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

        // 3. Accounts
        userAAccount = accountRepository.save(Account.builder()
                .user(userA)
                .name("User A Primary Account")
                .type(AccountType.BANK)
                .balance(new BigDecimal("25000.0000"))
                .currency("INR")
                .build());

        userBAccount = accountRepository.save(Account.builder()
                .user(userB)
                .name("User B Account")
                .type(AccountType.CASH)
                .balance(new BigDecimal("10000.0000"))
                .currency("INR")
                .build());

        // 4. Categories
        userAExpenseCat = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Groceries")
                .type(CategoryType.EXPENSE)
                .build());

        userAIncomeCat = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Salary")
                .type(CategoryType.INCOME)
                .build());

        userBExpenseCat = categoryRepository.save(Category.builder()
                .user(userB)
                .name("Travel")
                .type(CategoryType.EXPENSE)
                .build());
    }

    @Test
    @DisplayName("TEST 1: Create Expense Transaction (201 Created)")
    void test1_CreateExpense_Returns201Created() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.of(2026, 9, 6))
                .description("Groceries")
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.accountId", is(userAAccount.getId().toString())))
                .andExpect(jsonPath("$.categoryId", is(userAExpenseCat.getId().toString())))
                .andExpect(jsonPath("$.categoryName", is("Groceries")))
                .andExpect(jsonPath("$.type", is("EXPENSE")))
                .andExpect(jsonPath("$.amount", is(500.00)))
                .andExpect(jsonPath("$.transactionDate", is("2026-09-06")))
                .andExpect(jsonPath("$.description", is("Groceries")));

        assertThat(transactionRepository.findByUserId(userA.getId())).hasSize(1);
    }

    @Test
    @DisplayName("TEST 2: Create Income Transaction (201 Created)")
    void test2_CreateIncome_Returns201Created() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAIncomeCat.getId())
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("50000.00"))
                .transactionDate(LocalDate.of(2026, 9, 1))
                .description("Salary")
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("INCOME")))
                .andExpect(jsonPath("$.amount", is(50000.00)))
                .andExpect(jsonPath("$.description", is("Salary")));

        assertThat(transactionRepository.findByUserId(userA.getId())).hasSize(1);
    }

    @Test
    @DisplayName("TEST 3: Get My Transactions (User A receives only User A transactions)")
    void test3_GetMyTransactions_ReturnsUserATransactionsOnly() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("300.00"))
                .transactionDate(LocalDate.now())
                .description("Snacks")
                .build());

        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description", is("Snacks")));
    }

    @Test
    @DisplayName("TEST 4: Get Paginated Transactions (?page=0&size=20)")
    void test4_GetPaginatedTransactions_ReturnsPageMetadata() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build());

        mockMvc.perform(get("/api/v1/transactions?page=0&size=20")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    @DisplayName("TEST 5: Get Own Transaction (200 OK)")
    void test5_GetOwnTransaction_Returns200OK() throws Exception {
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("250.00"))
                .transactionDate(LocalDate.now())
                .description("Coffee")
                .build());

        mockMvc.perform(get("/api/v1/transactions/" + transaction.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(transaction.getId().toString())))
                .andExpect(jsonPath("$.description", is("Coffee")));
    }

    @Test
    @DisplayName("TEST 6: User B cannot access User A's transaction (403 Forbidden)")
    void test6_UserB_CannotAccessUserATransaction_Returns403Forbidden() throws Exception {
        Transaction transactionA = transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build());

        mockMvc.perform(get("/api/v1/transactions/" + transactionA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("TEST 7: User B cannot update User A's transaction (403 Forbidden)")
    void test7_UserB_CannotUpdateUserATransaction_Returns403Forbidden() throws Exception {
        Transaction transactionA = transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .description("Original")
                .build());

        TransactionRequest updateRequest = TransactionRequest.builder()
                .accountId(userBAccount.getId())
                .categoryId(userBExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("9999.00"))
                .transactionDate(LocalDate.now())
                .description("Hacked")
                .build();

        mockMvc.perform(put("/api/v1/transactions/" + transactionA.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        Transaction inDb = transactionRepository.findById(transactionA.getId()).orElseThrow();
        assertThat(inDb.getDescription()).isEqualTo("Original");
    }

    @Test
    @DisplayName("TEST 8: User B cannot delete User A's transaction (403 Forbidden)")
    void test8_UserB_CannotDeleteUserATransaction_Returns403Forbidden() throws Exception {
        Transaction transactionA = transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build());

        mockMvc.perform(delete("/api/v1/transactions/" + transactionA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        assertThat(transactionRepository.findById(transactionA.getId())).isPresent();
    }

    @Test
    @DisplayName("TEST 9: User A updates own transaction (200 OK, account balance updated)")
    void test9_UserA_UpdatesOwnTransaction_Returns200OK() throws Exception {
        // Create initial expense transaction 500
        TransactionRequest createReq = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .description("Dinner")
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID txId = UUID.fromString(objectMapper.readTree(responseStr).get("id").asText());

        // Update amount from 500 to 800
        TransactionRequest updateReq = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("800.00"))
                .transactionDate(LocalDate.now())
                .description("Fancy Dinner")
                .build();

        mockMvc.perform(put("/api/v1/transactions/" + txId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(800.00)))
                .andExpect(jsonPath("$.description", is("Fancy Dinner")));

        // Balance check: 25000 - 800 = 24200
        Account updatedAcc = accountRepository.findById(userAAccount.getId()).orElseThrow();
        assertThat(updatedAcc.getBalance()).isEqualByComparingTo("24200.0000");
    }

    @Test
    @DisplayName("TEST 10: User A deletes own transaction (204 No Content, balance restored)")
    void test10_UserA_DeletesOwnTransaction_RestoresBalance() throws Exception {
        // Create expense 500 (balance becomes 24500)
        TransactionRequest createReq = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID txId = UUID.fromString(objectMapper.readTree(responseStr).get("id").asText());

        // Delete transaction
        mockMvc.perform(delete("/api/v1/transactions/" + txId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        assertThat(transactionRepository.findById(txId)).isEmpty();

        // Balance restored to 25000
        Account restoredAcc = accountRepository.findById(userAAccount.getId()).orElseThrow();
        assertThat(restoredAcc.getBalance()).isEqualByComparingTo("25000.0000");
    }

    @Test
    @DisplayName("TEST 11: No JWT (Unauthenticated request returns 401 Unauthorized)")
    void test11_NoJwt_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TEST 12: Invalid Amount (0 or negative returns 400 Bad Request)")
    void test12_InvalidAmount_Returns400BadRequest() throws Exception {
        TransactionRequest zeroRequest = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(BigDecimal.ZERO)
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroRequest)))
                .andExpect(status().isBadRequest());

        TransactionRequest negativeRequest = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("-500.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 13: Invalid Type (Returns 400 Bad Request)")
    void test13_InvalidType_Returns400BadRequest() throws Exception {
        String invalidJson = String.format("""
                {
                  "accountId": "%s",
                  "categoryId": "%s",
                  "type": "INVALID",
                  "amount": 500,
                  "transactionDate": "2026-09-06"
                }
                """, userAAccount.getId(), userAExpenseCat.getId());

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 14: Category/Type Mismatch (EXPENSE category with INCOME transaction returns 400)")
    void test14_CategoryTypeMismatch_Returns400BadRequest() throws Exception {
        TransactionRequest mismatchRequest = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId()) // EXPENSE category
                .type(TransactionType.INCOME)       // INCOME transaction type mismatch
                .amount(new BigDecimal("1000.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("incompatible")));
    }

    @Test
    @DisplayName("TEST 15: Cross-User Account (User A JWT but User B accountId returns 403 Forbidden)")
    void test15_CrossUserAccount_Returns403Forbidden() throws Exception {
        TransactionRequest crossAccountRequest = TransactionRequest.builder()
                .accountId(userBAccount.getId()) // Belongs to User B
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossAccountRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 16: Cross-User Category (User A JWT but User B categoryId returns 403 Forbidden)")
    void test16_CrossUserCategory_Returns403Forbidden() throws Exception {
        TransactionRequest crossCategoryRequest = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userBExpenseCat.getId()) // Belongs to User B
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossCategoryRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 17: Client userId manipulation is ignored; JWT identity determines ownership")
    void test17_ClientUserIdManipulation_IsIgnored() throws Exception {
        String spoofedJson = String.format("""
                {
                  "accountId": "%s",
                  "categoryId": "%s",
                  "type": "EXPENSE",
                  "amount": 150,
                  "transactionDate": "2026-09-06",
                  "userId": "%s"
                }
                """, userAAccount.getId(), userAExpenseCat.getId(), userB.getId());

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spoofedJson))
                .andExpect(status().isCreated());

        assertThat(transactionRepository.findByUserId(userA.getId())).hasSize(1);
        assertThat(transactionRepository.findByUserId(userB.getId())).isEmpty();
    }

    @Test
    @DisplayName("TEST 18: Idempotency (Same clientOperationId sent twice creates exactly 1 transaction)")
    void test18_Idempotency_SameClientOperationId_CreatesOnlyOneTransaction() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .clientOperationId("op-uuid-12345")
                .build();

        // Call 1
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientOperationId", is("op-uuid-12345")));

        // Call 2 (Duplicate operation ID)
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientOperationId", is("op-uuid-12345")));

        // Verify only 1 transaction in DB and balance reduced only once (25000 - 500 = 24500)
        assertThat(transactionRepository.findByUserId(userA.getId())).hasSize(1);
        Account acc = accountRepository.findById(userAAccount.getId()).orElseThrow();
        assertThat(acc.getBalance()).isEqualByComparingTo("24500.0000");
    }

    @Test
    @DisplayName("TEST 19: Account Balance Verification (Starting 25000 -> Expense 500 = 24500 -> Income 2000 = 26500)")
    void test19_AccountBalance_CalculatesCorrectly() throws Exception {
        // Step 1: Initial balance = 25000
        Account accInitial = accountRepository.findById(userAAccount.getId()).orElseThrow();
        assertThat(accInitial.getBalance()).isEqualByComparingTo("25000.0000");

        // Step 2: Create EXPENSE 500
        TransactionRequest expenseReq = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseReq)))
                .andExpect(status().isCreated());

        Account accAfterExpense = accountRepository.findById(userAAccount.getId()).orElseThrow();
        assertThat(accAfterExpense.getBalance()).isEqualByComparingTo("24500.0000");

        // Step 3: Create INCOME 2000
        TransactionRequest incomeReq = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAIncomeCat.getId())
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("2000.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeReq)))
                .andExpect(status().isCreated());

        Account accAfterIncome = accountRepository.findById(userAAccount.getId()).orElseThrow();
        assertThat(accAfterIncome.getBalance()).isEqualByComparingTo("26500.0000");
    }
}
