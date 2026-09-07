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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test suite verifying Phase 3.4 Expense Management Integration & Finalization requirements.
 * Covers 30 comprehensive integration test scenarios for Accounts, Categories, and Transactions.
 */
import com.expenseguard.ExpenseGuardApplication;

@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseManagementIntegrationTest {

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
    private Account userAAccount2;
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

        // 3. Accounts for User A
        userAAccount = accountRepository.save(Account.builder()
                .user(userA)
                .name("User A Checking")
                .type(AccountType.BANK)
                .balance(new BigDecimal("25000.0000"))
                .currency("INR")
                .build());

        userAAccount2 = accountRepository.save(Account.builder()
                .user(userA)
                .name("User A Savings")
                .type(AccountType.SAVINGS)
                .balance(new BigDecimal("10000.0000"))
                .currency("INR")
                .build());

        // 4. Accounts for User B
        userBAccount = accountRepository.save(Account.builder()
                .user(userB)
                .name("User B Wallet")
                .type(AccountType.CASH)
                .balance(new BigDecimal("5000.0000"))
                .currency("INR")
                .build());

        // 5. Categories for User A
        userAExpenseCat = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Food")
                .type(CategoryType.EXPENSE)
                .build());

        userAIncomeCat = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Salary")
                .type(CategoryType.INCOME)
                .build());

        // 6. Categories for User B
        userBExpenseCat = categoryRepository.save(Category.builder()
                .user(userB)
                .name("Travel")
                .type(CategoryType.EXPENSE)
                .build());
    }

    @Test
    @DisplayName("TEST 1: Create expense transaction (201 Created)")
    void test1_CreateExpense_Returns201() throws Exception {
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
                .andExpect(jsonPath("$.type", is("EXPENSE")))
                .andExpect(jsonPath("$.amount", is(500.00)));
    }

    @Test
    @DisplayName("TEST 2: Create income transaction (201 Created)")
    void test2_CreateIncome_Returns201() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAIncomeCat.getId())
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("50000.00"))
                .transactionDate(LocalDate.of(2026, 9, 1))
                .description("Monthly Salary")
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("INCOME")))
                .andExpect(jsonPath("$.amount", is(50000.00)));
    }

    @Test
    @DisplayName("TEST 3: List own transactions (200 OK)")
    void test3_ListOwnTransactions_ReturnsUserATransactions() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("200.00"))
                .transactionDate(LocalDate.now())
                .description("Coffee")
                .build());

        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description", is("Coffee")));
    }

    @Test
    @DisplayName("TEST 4: Pagination metadata")
    void test4_Pagination_ReturnsCorrectMetadata() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build());

        mockMvc.perform(get("/api/v1/transactions?page=0&size=10")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    @DisplayName("TEST 5: Filter by type (?type=EXPENSE)")
    void test5_FilterByType_ReturnsFilteredTransactions() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("300.00"))
                .transactionDate(LocalDate.now())
                .description("Expense 1")
                .build());

        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAIncomeCat)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("2000.00"))
                .transactionDate(LocalDate.now())
                .description("Income 1")
                .build());

        mockMvc.perform(get("/api/v1/transactions?type=EXPENSE")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description", is("Expense 1")));
    }

    @Test
    @DisplayName("TEST 6: Filter by category (?categoryId={id})")
    void test6_FilterByCategory_ReturnsFilteredTransactions() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("150.00"))
                .transactionDate(LocalDate.now())
                .description("Food Tx")
                .build());

        mockMvc.perform(get("/api/v1/transactions?categoryId=" + userAExpenseCat.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description", is("Food Tx")));
    }

    @Test
    @DisplayName("TEST 7: Filter by account (?accountId={id})")
    void test7_FilterByAccount_ReturnsFilteredTransactions() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount2)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("400.00"))
                .transactionDate(LocalDate.now())
                .description("Savings Tx")
                .build());

        mockMvc.perform(get("/api/v1/transactions?accountId=" + userAAccount2.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description", is("Savings Tx")));
    }

    @Test
    @DisplayName("TEST 8: Filter by date range (?fromDate=...&toDate=...)")
    void test8_FilterByDateRange_ReturnsFilteredTransactions() throws Exception {
        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.of(2026, 9, 5))
                .description("Sep 5 Tx")
                .build());

        transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("200.00"))
                .transactionDate(LocalDate.of(2026, 8, 15))
                .description("Aug 15 Tx")
                .build());

        mockMvc.perform(get("/api/v1/transactions?fromDate=2026-09-01&toDate=2026-09-30")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description", is("Sep 5 Tx")));
    }

    @Test
    @DisplayName("TEST 9: Get own transaction by ID (200 OK)")
    void test9_GetOwnTransaction_Returns200() throws Exception {
        Transaction tx = transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("50.00"))
                .transactionDate(LocalDate.now())
                .description("Tea")
                .build());

        mockMvc.perform(get("/api/v1/transactions/" + tx.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(tx.getId().toString())));
    }

    @Test
    @DisplayName("TEST 10: Cross-user transaction access returns 403 Forbidden")
    void test10_CrossUserTransactionAccess_Returns403() throws Exception {
        Transaction txA = transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build());

        mockMvc.perform(get("/api/v1/transactions/" + txA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 11: Cross-user transaction update returns 403 Forbidden")
    void test11_CrossUserTransactionUpdate_Returns403() throws Exception {
        Transaction txA = transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build());

        TransactionRequest updateReq = TransactionRequest.builder()
                .accountId(userBAccount.getId())
                .categoryId(userBExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("9999.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(put("/api/v1/transactions/" + txA.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 12: Cross-user transaction delete returns 403 Forbidden")
    void test12_CrossUserTransactionDelete_Returns403() throws Exception {
        Transaction txA = transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build());

        mockMvc.perform(delete("/api/v1/transactions/" + txA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 13: Cross-user account reference returns 403 Forbidden")
    void test13_CrossUserAccountReference_Returns403() throws Exception {
        TransactionRequest req = TransactionRequest.builder()
                .accountId(userBAccount.getId()) // User B account
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 14: Cross-user category reference returns 403 Forbidden")
    void test14_CrossUserCategoryReference_Returns403() throws Exception {
        TransactionRequest req = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userBExpenseCat.getId()) // User B category
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 15: Invalid amount (< 0) returns 400 Bad Request")
    void test15_NegativeAmount_Returns400() throws Exception {
        TransactionRequest req = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("-100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 16: Zero amount (0) returns 400 Bad Request")
    void test16_ZeroAmount_Returns400() throws Exception {
        TransactionRequest req = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(BigDecimal.ZERO)
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 17: Invalid transaction type returns 400 Bad Request")
    void test17_InvalidTransactionType_Returns400() throws Exception {
        String json = String.format("""
                {
                  "accountId": "%s",
                  "categoryId": "%s",
                  "type": "INVALID_TYPE",
                  "amount": 100,
                  "transactionDate": "2026-09-06"
                }
                """, userAAccount.getId(), userAExpenseCat.getId());

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 18: Missing account returns 400 Bad Request")
    void test18_MissingAccount_Returns400() throws Exception {
        TransactionRequest req = TransactionRequest.builder()
                .accountId(null)
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 19: Missing category returns 400 Bad Request")
    void test19_MissingCategory_Returns400() throws Exception {
        TransactionRequest req = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(null)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 20: Category/type mismatch returns 400 Bad Request")
    void test20_CategoryTypeMismatch_Returns400() throws Exception {
        TransactionRequest req = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId()) // EXPENSE category
                .type(TransactionType.INCOME)       // INCOME type
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 21: No JWT returns 401 Unauthorized")
    void test21_NoJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TEST 22: Client userId spoofing is ignored")
    void test22_ClientUserIdSpoofing_IsIgnored() throws Exception {
        String spoofedJson = String.format("""
                {
                  "accountId": "%s",
                  "categoryId": "%s",
                  "type": "EXPENSE",
                  "amount": 100,
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
    @DisplayName("TEST 23: Idempotency creates no duplicate transaction")
    void test23_Idempotency_NoDuplicateTransaction() throws Exception {
        TransactionRequest req = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .clientOperationId("op-test-23")
                .build();

        // 1st attempt
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // 2nd attempt with same clientOperationId
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        assertThat(transactionRepository.findByUserId(userA.getId())).hasSize(1);
    }

    @Test
    @DisplayName("TEST 24: Idempotency causes no duplicate balance impact")
    void test24_Idempotency_NoDuplicateBalanceImpact() throws Exception {
        TransactionRequest req = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .clientOperationId("op-test-24")
                .build();

        // Initial balance 25000
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        Account acc = accountRepository.findById(userAAccount.getId()).orElseThrow();
        assertThat(acc.getBalance()).isEqualByComparingTo("24500.0000");
    }

    @Test
    @DisplayName("TEST 25: Update transaction balance calculation")
    void test25_UpdateTransactionBalance() throws Exception {
        Transaction tx = transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build());

        // Initial balance: 25000 - 500 = 24500
        userAAccount.setBalance(new BigDecimal("24500.0000"));
        accountRepository.save(userAAccount);

        TransactionRequest updateReq = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("1000.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(put("/api/v1/transactions/" + tx.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        // Revert 500 (+500 -> 25000) then apply 1000 (-1000 -> 24000)
        Account acc = accountRepository.findById(userAAccount.getId()).orElseThrow();
        assertThat(acc.getBalance()).isEqualByComparingTo("24000.0000");
    }

    @Test
    @DisplayName("TEST 26: Delete transaction balance calculation")
    void test26_DeleteTransactionBalance() throws Exception {
        Transaction tx = transactionRepository.save(Transaction.builder()
                .user(userA)
                .account(userAAccount)
                .category(userAExpenseCat)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build());

        userAAccount.setBalance(new BigDecimal("24500.0000"));
        accountRepository.save(userAAccount);

        mockMvc.perform(delete("/api/v1/transactions/" + tx.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        Account acc = accountRepository.findById(userAAccount.getId()).orElseThrow();
        assertThat(acc.getBalance()).isEqualByComparingTo("25000.0000");
    }

    @Test
    @DisplayName("TEST 27: Move transaction between accounts (Account A -> Account B)")
    void test27_MoveTransactionBetweenAccounts() throws Exception {
        // Create expense 500 on Account 1 (balance becomes 24500)
        TransactionRequest createReq = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build();

        String respStr = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID txId = UUID.fromString(objectMapper.readTree(respStr).get("id").asText());

        // Move transaction to Account 2 (Initial balance 10000)
        TransactionRequest moveReq = TransactionRequest.builder()
                .accountId(userAAccount2.getId()) // Moved to Account 2
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(put("/api/v1/transactions/" + txId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveReq)))
                .andExpect(status().isOk());

        // Account 1 balance restored to 25000
        Account acc1 = accountRepository.findById(userAAccount.getId()).orElseThrow();
        assertThat(acc1.getBalance()).isEqualByComparingTo("25000.0000");

        // Account 2 balance reduced: 10000 - 500 = 9500
        Account acc2 = accountRepository.findById(userAAccount2.getId()).orElseThrow();
        assertThat(acc2.getBalance()).isEqualByComparingTo("9500.0000");
    }

    @Test
    @DisplayName("TEST 28: Final balance consistency sequence (Initial 25k -> Expense 500 = 24.5k -> Income 2k = 26.5k -> Update expense to 1k = 26k -> Delete expense = 27k -> Delete income = 25k)")
    void test28_FinalBalanceConsistencySequence() throws Exception {
        // Step 1: Initial balance 25000
        assertThat(accountRepository.findById(userAAccount.getId()).orElseThrow().getBalance()).isEqualByComparingTo("25000.0000");

        // Step 2: Create EXPENSE 500 -> 24500
        TransactionRequest expReq = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build();

        String expRespStr = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID expId = UUID.fromString(objectMapper.readTree(expRespStr).get("id").asText());
        assertThat(accountRepository.findById(userAAccount.getId()).orElseThrow().getBalance()).isEqualByComparingTo("24500.0000");

        // Step 3: Create INCOME 2000 -> 26500
        TransactionRequest incReq = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAIncomeCat.getId())
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("2000.00"))
                .transactionDate(LocalDate.now())
                .build();

        String incRespStr = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incReq)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID incId = UUID.fromString(objectMapper.readTree(incRespStr).get("id").asText());
        assertThat(accountRepository.findById(userAAccount.getId()).orElseThrow().getBalance()).isEqualByComparingTo("26500.0000");

        // Step 4: Update EXPENSE 500 -> 1000 -> 26000
        TransactionRequest expUpdate = TransactionRequest.builder()
                .accountId(userAAccount.getId())
                .categoryId(userAExpenseCat.getId())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("1000.00"))
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(put("/api/v1/transactions/" + expId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expUpdate)))
                .andExpect(status().isOk());
        assertThat(accountRepository.findById(userAAccount.getId()).orElseThrow().getBalance()).isEqualByComparingTo("26000.0000");

        // Step 5: Delete EXPENSE -> 27000
        mockMvc.perform(delete("/api/v1/transactions/" + expId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());
        assertThat(accountRepository.findById(userAAccount.getId()).orElseThrow().getBalance()).isEqualByComparingTo("27000.0000");

        // Step 6: Delete INCOME -> 25000 (Final balance equals original balance!)
        mockMvc.perform(delete("/api/v1/transactions/" + incId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());
        assertThat(accountRepository.findById(userAAccount.getId()).orElseThrow().getBalance()).isEqualByComparingTo("25000.0000");
    }

    @Test
    @DisplayName("TEST 29: Nonexistent transaction returns 404 Not Found")
    void test29_NonexistentTransaction_Returns404() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/transactions/" + randomId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TEST 30: Excessive page size handled safely (size=500 capped to 100)")
    void test30_ExcessivePageSize_IsCappedSafely() throws Exception {
        mockMvc.perform(get("/api/v1/transactions?size=500")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(100)));
    }
}
