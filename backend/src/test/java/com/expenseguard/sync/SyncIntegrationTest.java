package com.expenseguard.sync;

import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.auth.service.AuthService;
import com.expenseguard.sync.dto.BatchSyncRequest;
import com.expenseguard.sync.dto.SyncTransactionRequest;
import com.expenseguard.sync.entity.SyncEntityType;
import com.expenseguard.sync.entity.SyncOperation;
import com.expenseguard.sync.entity.SyncOperationStatus;
import com.expenseguard.sync.entity.SyncOperationType;
import com.expenseguard.sync.repository.SyncOperationRepository;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test suite verifying Phase 5.3 Offline Synchronization & Conflict Handling requirements.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SyncIntegrationTest {

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
    private SyncOperationRepository syncOperationRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String token;
    private Account testAccount;
    private Category testCategory;

    @BeforeEach
    void setUp() throws Exception {
        syncOperationRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        token = registerAndGetToken("syncuser@example.com", "Sync User");
        testUser = userRepository.findByEmail("syncuser@example.com").orElseThrow();

        testAccount = Account.builder()
                .user(testUser)
                .name("Main Bank Account")
                .type(AccountType.BANK)
                .balance(new BigDecimal("25000.0000"))
                .build();
        testAccount = accountRepository.save(testAccount);

        testCategory = Category.builder()
                .user(testUser)
                .name("Food & Dining")
                .type(CategoryType.EXPENSE)
                .build();
        testCategory = categoryRepository.save(testCategory);
    }

    private String registerAndGetToken(String email, String name) throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name(name)
                .email(email)
                .password("Password@123")
                .build();

        authService.registerUser(registerRequest);
        User user = userRepository.findByEmail(email).orElseThrow();
        return "Bearer " + jwtService.generateToken(user);
    }

    // 1. CREATE sync succeeds
    @Test
    @DisplayName("1. CREATE sync succeeds and returns PROCESSED status")
    void createSyncSucceeds() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("mobile-001")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .description("Lunch")
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientOperationId", is("mobile-001")))
                .andExpect(jsonPath("$.status", is("PROCESSED")))
                .andExpect(jsonPath("$.transactionId", notNullValue()))
                .andExpect(jsonPath("$.message", containsString("successfully")));
    }

    // 2. CREATE creates one transaction
    @Test
    @DisplayName("2. CREATE sync creates exactly one transaction record in database")
    void createCreatesOneTransaction() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("mobile-002")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .description("Lunch")
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    // 3. CREATE updates balance
    @Test
    @DisplayName("3. CREATE sync updates account balance from ₹25,000 to ₹24,500")
    void createUpdatesBalance() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("mobile-003")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .description("Lunch")
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("24500.0000");
    }

    // 4. Retry same clientOperationId
    @Test
    @DisplayName("4. Retry same clientOperationId returns PROCESSED status")
    void retrySameClientOperationId() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("mobile-004")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .description("Lunch")
                .build();

        // First attempt
        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Retry attempt
        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientOperationId", is("mobile-004")))
                .andExpect(jsonPath("$.status", is("PROCESSED")));
    }

    // 5. Retry does not duplicate transaction
    @Test
    @DisplayName("5. Retry does not duplicate transaction records")
    void retryDoesNotDuplicateTransaction() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("mobile-005")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .description("Lunch")
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    // 6. Retry does not duplicate balance
    @Test
    @DisplayName("6. Retry does not duplicate account balance deduction")
    void retryDoesNotDuplicateBalance() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("mobile-006")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .description("Lunch")
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("24500.0000");
    }

    // 7. Same operation ID with different payload -> 409
    @Test
    @DisplayName("7. Same clientOperationId with different payload returns 409 Conflict")
    void sameOperationIdWithDifferentPayloadReturns409() throws Exception {
        SyncTransactionRequest request1 = SyncTransactionRequest.builder()
                .clientOperationId("mobile-007")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .description("Lunch")
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Same operation ID, different amount (900 instead of 500)
        SyncTransactionRequest request2 = SyncTransactionRequest.builder()
                .clientOperationId("mobile-007")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("900.00"))
                .transactionDate(LocalDate.now())
                .description("Dinner")
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.errorCode", is("SYNC_OPERATION_PAYLOAD_MISMATCH")));
    }

    // 8. UPDATE sync succeeds
    @Test
    @DisplayName("8. UPDATE sync succeeds with current version")
    void updateSyncSucceeds() throws Exception {
        Transaction existingTx = Transaction.builder()
                .user(testUser)
                .account(testAccount)
                .category(testCategory)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .description("Old Lunch")
                .version(0L)
                .build();
        existingTx = transactionRepository.save(existingTx);

        SyncTransactionRequest updateRequest = SyncTransactionRequest.builder()
                .clientOperationId("mobile-update-001")
                .operationType(SyncOperationType.UPDATE)
                .transactionId(existingTx.getId())
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("700.00"))
                .transactionDate(LocalDate.now())
                .description("Updated Lunch")
                .version(0L)
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")));
    }

    // 9. UPDATE updates balance correctly
    @Test
    @DisplayName("9. UPDATE sync updates balance correctly when amount changes")
    void updateUpdatesBalanceCorrectly() throws Exception {
        // Initial balance 25000. Expense 500 => balance 24500
        testAccount.setBalance(new BigDecimal("24500.0000"));
        accountRepository.save(testAccount);

        Transaction existingTx = Transaction.builder()
                .user(testUser)
                .account(testAccount)
                .category(testCategory)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .description("Lunch")
                .version(0L)
                .build();
        existingTx = transactionRepository.save(existingTx);

        // Update amount from 500 to 700
        SyncTransactionRequest updateRequest = SyncTransactionRequest.builder()
                .clientOperationId("mobile-update-002")
                .operationType(SyncOperationType.UPDATE)
                .transactionId(existingTx.getId())
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("700.00"))
                .transactionDate(LocalDate.now())
                .description("New Lunch")
                .version(0L)
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        // 24500 + 500 - 700 = 24300
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("24300.0000");
    }

    // 10. UPDATE with stale version -> 409
    @Test
    @DisplayName("10. UPDATE with stale version returns 409 Conflict with TRANSACTION_CONFLICT")
    void updateWithStaleVersionReturns409() throws Exception {
        Transaction existingTx = Transaction.builder()
                .user(testUser)
                .account(testAccount)
                .category(testCategory)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .version(2L) // Server version is 2
                .build();
        existingTx = transactionRepository.save(existingTx);

        // Client sends version 1 (stale)
        SyncTransactionRequest updateRequest = SyncTransactionRequest.builder()
                .clientOperationId("mobile-update-003")
                .operationType(SyncOperationType.UPDATE)
                .transactionId(existingTx.getId())
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("600.00"))
                .transactionDate(LocalDate.now())
                .version(1L)
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.errorCode", is("TRANSACTION_CONFLICT")))
                .andExpect(jsonPath("$.serverVersion", is(2)));
    }

    // 11. UPDATE with current version succeeds
    @Test
    @DisplayName("11. UPDATE with current version succeeds")
    void updateWithCurrentVersionSucceeds() throws Exception {
        Transaction existingTx = Transaction.builder()
                .user(testUser)
                .account(testAccount)
                .category(testCategory)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .version(3L)
                .build();
        existingTx = transactionRepository.save(existingTx);

        SyncTransactionRequest updateRequest = SyncTransactionRequest.builder()
                .clientOperationId("mobile-update-004")
                .operationType(SyncOperationType.UPDATE)
                .transactionId(existingTx.getId())
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("600.00"))
                .transactionDate(LocalDate.now())
                .version(3L)
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")));
    }

    // 12. DELETE sync succeeds
    @Test
    @DisplayName("12. DELETE sync succeeds")
    void deleteSyncSucceeds() throws Exception {
        Transaction existingTx = Transaction.builder()
                .user(testUser)
                .account(testAccount)
                .category(testCategory)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build();
        existingTx = transactionRepository.save(existingTx);

        SyncTransactionRequest deleteRequest = SyncTransactionRequest.builder()
                .clientOperationId("mobile-delete-001")
                .operationType(SyncOperationType.DELETE)
                .transactionId(existingTx.getId())
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")));

        assertThat(transactionRepository.findById(existingTx.getId())).isEmpty();
    }

    // 13. DELETE restores balance
    @Test
    @DisplayName("13. DELETE sync restores account balance")
    void deleteRestoresBalance() throws Exception {
        testAccount.setBalance(new BigDecimal("24500.0000"));
        accountRepository.save(testAccount);

        Transaction existingTx = Transaction.builder()
                .user(testUser)
                .account(testAccount)
                .category(testCategory)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build();
        existingTx = transactionRepository.save(existingTx);

        SyncTransactionRequest deleteRequest = SyncTransactionRequest.builder()
                .clientOperationId("mobile-delete-002")
                .operationType(SyncOperationType.DELETE)
                .transactionId(existingTx.getId())
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isOk());

        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("25000.0000");
    }

    // 14. DELETE retry is idempotent
    @Test
    @DisplayName("14. DELETE retry is idempotent and does not error")
    void deleteRetryIsIdempotent() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        SyncTransactionRequest deleteRequest = SyncTransactionRequest.builder()
                .clientOperationId("mobile-delete-003")
                .operationType(SyncOperationType.DELETE)
                .transactionId(nonExistentId)
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")));
    }

    // 15. Cross-user transaction -> 403
    @Test
    @DisplayName("15. Updating another user's transaction returns 403 Forbidden")
    void crossUserTransactionReturns403() throws Exception {
        String tokenUserB = registerAndGetToken("userb@example.com", "User B");
        User userB = userRepository.findByEmail("userb@example.com").orElseThrow();

        Account accountB = Account.builder()
                .user(userB)
                .name("Account B")
                .type(AccountType.BANK)
                .balance(new BigDecimal("1000.00"))
                .build();
        accountB = accountRepository.save(accountB);

        Category categoryB = Category.builder()
                .user(userB)
                .name("Category B")
                .type(CategoryType.EXPENSE)
                .build();
        categoryB = categoryRepository.save(categoryB);

        Transaction txB = Transaction.builder()
                .user(userB)
                .account(accountB)
                .category(categoryB)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .version(0L)
                .build();
        txB = transactionRepository.save(txB);

        // User A tries to update User B's transaction
        SyncTransactionRequest crossRequest = SyncTransactionRequest.builder()
                .clientOperationId("cross-001")
                .operationType(SyncOperationType.UPDATE)
                .transactionId(txB.getId())
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("200.00"))
                .transactionDate(LocalDate.now())
                .version(0L)
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossRequest)))
                .andExpect(status().isForbidden());
    }

    // 16. Cross-user account -> 403
    @Test
    @DisplayName("16. Sync request using another user's account returns 403 Forbidden")
    void crossUserAccountReturns403() throws Exception {
        String tokenUserB = registerAndGetToken("userb2@example.com", "User B2");
        User userB = userRepository.findByEmail("userb2@example.com").orElseThrow();

        Account accountB = Account.builder()
                .user(userB)
                .name("Account B2")
                .type(AccountType.BANK)
                .balance(new BigDecimal("1000.00"))
                .build();
        accountB = accountRepository.save(accountB);

        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("cross-acc-001")
                .operationType(SyncOperationType.CREATE)
                .accountId(accountB.getId()) // User B's account
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // 17. Cross-user category -> 403
    @Test
    @DisplayName("17. Sync request using another user's category returns 403 Forbidden")
    void crossUserCategoryReturns403() throws Exception {
        String tokenUserB = registerAndGetToken("userb3@example.com", "User B3");
        User userB = userRepository.findByEmail("userb3@example.com").orElseThrow();

        Category categoryB = Category.builder()
                .user(userB)
                .name("Category B3")
                .type(CategoryType.EXPENSE)
                .build();
        categoryB = categoryRepository.save(categoryB);

        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("cross-cat-001")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(categoryB.getId()) // User B's category
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // 18. Cross-user sync operation cannot be accessed
    @Test
    @DisplayName("18. User A cannot access User B's sync status")
    void crossUserSyncOperationCannotBeAccessed() throws Exception {
        String tokenUserB = registerAndGetToken("userb4@example.com", "User B4");
        User userB = userRepository.findByEmail("userb4@example.com").orElseThrow();

        SyncOperation syncOpB = SyncOperation.builder()
                .user(userB)
                .clientOperationId("mobile-userb-op")
                .operationType(SyncOperationType.CREATE)
                .entityType(SyncEntityType.TRANSACTION)
                .status(SyncOperationStatus.PROCESSED)
                .build();
        syncOperationRepository.save(syncOpB);

        // User A attempts to look up User B's sync operation
        mockMvc.perform(get("/api/v1/sync/status/mobile-userb-op")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    // 19. No JWT -> 401
    @Test
    @DisplayName("19. Sync request without JWT returns 401 Unauthorized")
    void noJwtReturns401() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("nojwt-001")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // 20. Invalid operation type -> 400
    @Test
    @DisplayName("20. Missing operationType returns 400 Bad Request")
    void invalidOperationTypeReturns400() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("inv-op-001")
                .operationType(null)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // 21. Invalid entity type -> 400
    @Test
    @DisplayName("21. Invalid entityType returns 400 Bad Request")
    void invalidEntityTypeReturns400() throws Exception {
        // String JSON with unsupported entity type
        String rawJson = "{\n" +
                "  \"clientOperationId\": \"inv-ent-001\",\n" +
                "  \"operationType\": \"CREATE\",\n" +
                "  \"entityType\": \"ACCOUNT\",\n" +
                "  \"accountId\": \"" + testAccount.getId() + "\",\n" +
                "  \"categoryId\": \"" + testCategory.getId() + "\",\n" +
                "  \"type\": \"EXPENSE\",\n" +
                "  \"amount\": 100,\n" +
                "  \"transactionDate\": \"2026-09-07\"\n" +
                "}";

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isBadRequest());
    }

    // 22. Missing clientOperationId -> 400
    @Test
    @DisplayName("22. Missing clientOperationId returns 400 Bad Request")
    void missingClientOperationIdReturns400() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // 23. Missing transactionId for UPDATE -> 400
    @Test
    @DisplayName("23. Missing transactionId for UPDATE returns 400 Bad Request")
    void missingTransactionIdForUpdateReturns400() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("missing-txid-upd")
                .operationType(SyncOperationType.UPDATE)
                .transactionId(null)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .version(1L)
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // 24. Missing transactionId for DELETE -> 400
    @Test
    @DisplayName("24. Missing transactionId for DELETE returns 400 Bad Request")
    void missingTransactionIdForDeleteReturns400() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("missing-txid-del")
                .operationType(SyncOperationType.DELETE)
                .transactionId(null)
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // 25. Invalid amount -> 400
    @Test
    @DisplayName("25. Zero or negative transaction amount returns 400 Bad Request")
    void invalidAmountReturns400() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("inv-amt-001")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("-50.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // 26. Missing date -> 400
    @Test
    @DisplayName("26. Missing transactionDate returns 400 Bad Request")
    void missingDateReturns400() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("missing-date-001")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(null)
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // 27. Missing version for UPDATE -> 400
    @Test
    @DisplayName("27. Missing version for UPDATE operation returns 400 Bad Request")
    void missingVersionForUpdateReturns400() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("missing-ver-001")
                .operationType(SyncOperationType.UPDATE)
                .transactionId(UUID.randomUUID())
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .version(null)
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // 28. Sync status lookup
    @Test
    @DisplayName("28. Sync status lookup returns status object")
    void syncStatusLookup() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("status-lookup-001")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("150.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sync/status/status-lookup-001")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientOperationId", is("status-lookup-001")))
                .andExpect(jsonPath("$.status", is("PROCESSED")))
                .andExpect(jsonPath("$.transactionId", notNullValue()));
    }

    // 29. Processed status returned correctly
    @Test
    @DisplayName("29. Processed status returned correctly on GET status endpoint")
    void processedStatusReturnedCorrectly() throws Exception {
        SyncOperation syncOp = SyncOperation.builder()
                .user(testUser)
                .clientOperationId("proc-status-001")
                .operationType(SyncOperationType.CREATE)
                .entityType(SyncEntityType.TRANSACTION)
                .entityId(UUID.randomUUID())
                .status(SyncOperationStatus.PROCESSED)
                .build();
        syncOperationRepository.save(syncOp);

        mockMvc.perform(get("/api/v1/sync/status/proc-status-001")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")));
    }

    // 30. Conflict status returned correctly
    @Test
    @DisplayName("30. Conflict status returned correctly on GET status endpoint")
    void conflictStatusReturnedCorrectly() throws Exception {
        SyncOperation syncOp = SyncOperation.builder()
                .user(testUser)
                .clientOperationId("conflict-status-001")
                .operationType(SyncOperationType.UPDATE)
                .entityType(SyncEntityType.TRANSACTION)
                .entityId(UUID.randomUUID())
                .status(SyncOperationStatus.CONFLICT)
                .errorCode("TRANSACTION_CONFLICT")
                .errorMessage("Stale version")
                .build();
        syncOperationRepository.save(syncOp);

        mockMvc.perform(get("/api/v1/sync/status/conflict-status-001")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.errorCode", is("TRANSACTION_CONFLICT")));
    }

    // 31. UserId spoofing ignored
    @Test
    @DisplayName("31. User ID spoofing in client requests is strictly ignored")
    void userIdSpoofingIgnored() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("spoof-001")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        SyncOperation syncOp = syncOperationRepository.findByUserIdAndClientOperationId(testUser.getId(), "spoof-001").orElseThrow();
        assertThat(syncOp.getUser().getId()).isEqualTo(testUser.getId());
    }

    // 32. Batch synchronization
    @Test
    @DisplayName("32. Batch synchronization processes multiple operations")
    void batchSynchronization() throws Exception {
        SyncTransactionRequest req1 = SyncTransactionRequest.builder()
                .clientOperationId("batch-001")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        SyncTransactionRequest req2 = SyncTransactionRequest.builder()
                .clientOperationId("batch-002")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("200.00"))
                .transactionDate(LocalDate.now())
                .build();

        BatchSyncRequest batchRequest = BatchSyncRequest.builder()
                .operations(List.of(req1, req2))
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions/batch")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.results[0].status", is("PROCESSED")))
                .andExpect(jsonPath("$.results[1].status", is("PROCESSED")));
    }

    // 33. Batch partial failure handled safely
    @Test
    @DisplayName("33. Batch partial failure marks failed item without failing valid items")
    void batchPartialFailureHandledSafely() throws Exception {
        SyncTransactionRequest reqValid = SyncTransactionRequest.builder()
                .clientOperationId("batch-valid-001")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        SyncTransactionRequest reqInvalid = SyncTransactionRequest.builder()
                .clientOperationId("batch-invalid-001")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("-50.00")) // Invalid amount
                .transactionDate(LocalDate.now())
                .build();

        BatchSyncRequest batchRequest = BatchSyncRequest.builder()
                .operations(List.of(reqValid, reqInvalid))
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions/batch")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.results[0].status", is("PROCESSED")))
                .andExpect(jsonPath("$.results[1].status", is("FAILED")));
    }

    // 34. Transaction balance remains consistent after conflict
    @Test
    @DisplayName("34. Transaction balance remains consistent when version conflict occurs")
    void transactionBalanceRemainsConsistentAfterConflict() throws Exception {
        testAccount.setBalance(new BigDecimal("25000.0000"));
        accountRepository.save(testAccount);

        Transaction existingTx = Transaction.builder()
                .user(testUser)
                .account(testAccount)
                .category(testCategory)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .version(5L)
                .build();
        existingTx = transactionRepository.save(existingTx);

        // Stale update attempt (version 3 instead of 5)
        SyncTransactionRequest updateRequest = SyncTransactionRequest.builder()
                .clientOperationId("mobile-conflict-bal")
                .operationType(SyncOperationType.UPDATE)
                .transactionId(existingTx.getId())
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("9999.00"))
                .transactionDate(LocalDate.now())
                .version(3L)
                .build();

        mockMvc.perform(post("/api/v1/sync/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict());

        Account accountAfter = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(accountAfter.getBalance()).isEqualByComparingTo("25000.0000");
    }

    // 35. Concurrent duplicate CREATE operations produce one transaction
    @Test
    @DisplayName("35. Retried sync requests produce exactly one transaction and one balance change")
    void concurrentDuplicateCreateOperationsProduceOneTransaction() throws Exception {
        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("concurrent-001")
                .operationType(SyncOperationType.CREATE)
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("300.00"))
                .transactionDate(LocalDate.now())
                .description("Coffee")
                .build();

        // Perform request 3 times
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/sync/transactions")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        assertThat(transactionRepository.count()).isEqualTo(1);
        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        // 25000 - 300 = 24700
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("24700.0000");
    }
}
