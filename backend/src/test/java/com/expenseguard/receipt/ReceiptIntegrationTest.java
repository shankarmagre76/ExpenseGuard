package com.expenseguard.receipt;

import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.auth.service.AuthService;
import com.expenseguard.budget.repository.BudgetRepository;
import com.expenseguard.receipt.dto.ReceiptConfirmationRequest;
import com.expenseguard.receipt.entity.Receipt;
import com.expenseguard.receipt.entity.ReceiptStatus;
import com.expenseguard.receipt.repository.ReceiptRepository;
import com.expenseguard.receipt.storage.ReceiptStorageService;
import com.expenseguard.recurring.repository.RecurringTransactionRepository;
import com.expenseguard.transaction.entity.Account;
import com.expenseguard.transaction.entity.AccountType;
import com.expenseguard.transaction.entity.Category;
import com.expenseguard.transaction.entity.CategoryType;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test suite for Phase 5.2 Receipt Upload & OCR module.
 * Verifies 35 key test scenarios including file validation, secure storage, OCR extraction,
 * data confirmation, security isolation, physical file cleanup, and transaction decoupling.
 */
import com.expenseguard.ExpenseGuardApplication;

@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReceiptIntegrationTest {

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
    private BudgetRepository budgetRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private ReceiptStorageService storageService;

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
    private Account accountA;
    private Category categoryFoodA;

    @BeforeEach
    void setUp() {
        receiptRepository.deleteAll();
        recurringTransactionRepository.deleteAll();
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register User A
        RegisterRequest regA = RegisterRequest.builder()
                .name("User A")
                .email("receipt.usera@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(regA);
        userA = userRepository.findByEmail("receipt.usera@example.com").orElseThrow();
        tokenA = jwtService.generateToken(userA);

        // 2. Register User B
        RegisterRequest regB = RegisterRequest.builder()
                .name("User B")
                .email("receipt.userb@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(regB);
        userB = userRepository.findByEmail("receipt.userb@example.com").orElseThrow();
        tokenB = jwtService.generateToken(userB);

        // 3. Accounts & Categories for User A
        accountA = accountRepository.save(Account.builder()
                .user(userA)
                .name("Main Account")
                .type(AccountType.BANK)
                .balance(new BigDecimal("50000.0000"))
                .currency("INR")
                .build());

        categoryFoodA = categoryRepository.save(Category.builder()
                .user(userA)
                .name("Food & Dining")
                .type(CategoryType.EXPENSE)
                .build());
    }

    // --- Upload & Validation Tests ---

    @Test
    @DisplayName("TEST 1: Upload JPEG receipt (201 Created)")
    void test1_UploadJpegReceipt_Returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.jpg", "image/jpeg", "Receipt Content JPEG".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.fileName", is("receipt.jpg")))
                .andExpect(jsonPath("$.contentType", is("image/jpeg")))
                .andExpect(jsonPath("$.status", is("PROCESSED")));
    }

    @Test
    @DisplayName("TEST 2: Upload PNG receipt (201 Created)")
    void test2_UploadPngReceipt_Returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.png", "image/png", "Receipt Content PNG".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType", is("image/png")));
    }

    @Test
    @DisplayName("TEST 3: Upload PDF receipt (201 Created)")
    void test3_UploadPdfReceipt_Returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", "%PDF-1.4 Invoice Content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType", is("application/pdf")));
    }

    @Test
    @DisplayName("TEST 4: Empty file rejected (400 Bad Request)")
    void test4_EmptyFile_Returns400() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(emptyFile)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 5: Oversized file (>10MB) rejected (400 Bad Request)")
    void test5_OversizedFile_Returns400() throws Exception {
        byte[] largeBytes = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", largeBytes
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(largeFile)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 6: Unsupported content type rejected (400 Bad Request)")
    void test6_UnsupportedContentType_Returns400() throws Exception {
        MockMultipartFile scriptFile = new MockMultipartFile(
                "file", "script.sh", "application/x-sh", "echo 'hello'".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(scriptFile)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 7: Receipt stored successfully on disk")
    void test7_ReceiptStoredSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "Sample PNG Data".getBytes()
        );

        String response = mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String receiptIdStr = objectMapper.readTree(response).get("id").asText();
        Receipt receipt = receiptRepository.findById(UUID.fromString(receiptIdStr)).orElseThrow();

        assertThat(receipt.getStorageKey()).isNotNull();
        assertThat(storageService.exists(userA.getId(), receipt.getStorageKey())).isTrue();
    }

    @Test
    @DisplayName("TEST 8: Receipt status set to PROCESSED after successful OCR")
    void test8_ReceiptStatusProcessed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "Valid Receipt Content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PROCESSED")));
    }

    // --- OCR Extraction Tests ---

    @Test
    @DisplayName("TEST 9: OCR processing succeeds")
    void test9_OcrProcessingSucceeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "Valid Receipt".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PROCESSED")));
    }

    @Test
    @DisplayName("TEST 10: Merchant extracted correctly by OCR")
    void test10_MerchantExtracted() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "Receipt Content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantName", is("Supermarket Mega Store")));
    }

    @Test
    @DisplayName("TEST 11: Date extracted correctly by OCR")
    void test11_DateExtracted() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "Receipt Content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.extractedDate", is("2026-09-07")));
    }

    @Test
    @DisplayName("TEST 12: Amount extracted correctly by OCR")
    void test12_AmountExtracted() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "Receipt Content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.extractedAmount", is(1250.00)));
    }

    @Test
    @DisplayName("TEST 13: Confidence score returned by OCR")
    void test13_ConfidenceReturned() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "Receipt Content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confidence", is(0.95)));
    }

    @Test
    @DisplayName("TEST 14: Low confidence does not automatically create a transaction")
    void test14_LowConfidence_DoesNotCreateTransaction() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.jpg", "image/jpeg", "LOW_CONFIDENCE receipt content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confidence", is(0.45)));

        assertThat(transactionRepository.findAllByUserId(userA.getId())).isEmpty();
    }

    // --- Confirmation Tests ---

    @Test
    @DisplayName("TEST 15: Confirm OCR data (PUT /confirm)")
    void test15_ConfirmOcrData() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.jpg", "image/jpeg", "Receipt Data".getBytes()
        );

        String response = mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String receiptId = objectMapper.readTree(response).get("id").asText();

        ReceiptConfirmationRequest confirmReq = ReceiptConfirmationRequest.builder()
                .merchantName("Corrected Store Name")
                .transactionDate(LocalDate.of(2026, 9, 7))
                .amount(new BigDecimal("1500.00"))
                .build();

        mockMvc.perform(put("/api/v1/receipts/" + receiptId + "/confirm")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.merchantName", is("Corrected Store Name")))
                .andExpect(jsonPath("$.extractedAmount", is(1500.00)));
    }

    @Test
    @DisplayName("TEST 16: Corrected amount validated")
    void test16_CorrectedAmountValidated() throws Exception {
        Receipt receipt = receiptRepository.save(Receipt.builder()
                .user(userA).fileName("test.jpg").contentType("image/jpeg")
                .storageKey(UUID.randomUUID().toString()).fileSize(100L)
                .status(ReceiptStatus.PROCESSED).build());

        ReceiptConfirmationRequest confirmReq = ReceiptConfirmationRequest.builder()
                .merchantName("Valid Merchant")
                .transactionDate(LocalDate.of(2026, 9, 7))
                .amount(new BigDecimal("250.75"))
                .build();

        mockMvc.perform(put("/api/v1/receipts/" + receipt.getId() + "/confirm")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extractedAmount", is(250.75)));
    }

    @Test
    @DisplayName("TEST 17: Invalid amount (<=0) in confirmation rejected (400 Bad Request)")
    void test17_InvalidAmountInConfirmation_Returns400() throws Exception {
        Receipt receipt = receiptRepository.save(Receipt.builder()
                .user(userA).fileName("test.jpg").contentType("image/jpeg")
                .storageKey(UUID.randomUUID().toString()).fileSize(100L)
                .status(ReceiptStatus.PROCESSED).build());

        ReceiptConfirmationRequest confirmReq = ReceiptConfirmationRequest.builder()
                .merchantName("Merchant")
                .transactionDate(LocalDate.of(2026, 9, 7))
                .amount(new BigDecimal("-10.00")) // negative
                .build();

        mockMvc.perform(put("/api/v1/receipts/" + receipt.getId() + "/confirm")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isBadRequest());
    }

    // --- Ownership & Security Tests ---

    @Test
    @DisplayName("TEST 18: Own receipt retrieval (200 OK)")
    void test18_OwnReceiptRetrieval() throws Exception {
        Receipt receipt = receiptRepository.save(Receipt.builder()
                .user(userA).fileName("my_receipt.jpg").contentType("image/jpeg")
                .storageKey(UUID.randomUUID().toString()).fileSize(500L)
                .status(ReceiptStatus.PROCESSED).build());

        mockMvc.perform(get("/api/v1/receipts/" + receipt.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(receipt.getId().toString())));
    }

    @Test
    @DisplayName("TEST 19: Own receipt deletion (204 No Content)")
    void test19_OwnReceiptDeletion() throws Exception {
        String key = storageService.storeFile(userA.getId(), "del.jpg", "content".getBytes());
        Receipt receipt = receiptRepository.save(Receipt.builder()
                .user(userA).fileName("del.jpg").contentType("image/jpeg")
                .storageKey(key).fileSize(100L)
                .status(ReceiptStatus.PROCESSED).build());

        mockMvc.perform(delete("/api/v1/receipts/" + receipt.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        assertThat(receiptRepository.findById(receipt.getId())).isEmpty();
    }

    @Test
    @DisplayName("TEST 20: Cross-user GET returns 403 Forbidden")
    void test20_CrossUserGet_Returns403() throws Exception {
        Receipt receiptA = receiptRepository.save(Receipt.builder()
                .user(userA).fileName("receiptA.jpg").contentType("image/jpeg")
                .storageKey(UUID.randomUUID().toString()).fileSize(100L)
                .status(ReceiptStatus.PROCESSED).build());

        mockMvc.perform(get("/api/v1/receipts/" + receiptA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 21: Cross-user DELETE returns 403 Forbidden")
    void test21_CrossUserDelete_Returns403() throws Exception {
        Receipt receiptA = receiptRepository.save(Receipt.builder()
                .user(userA).fileName("receiptA.jpg").contentType("image/jpeg")
                .storageKey(UUID.randomUUID().toString()).fileSize(100L)
                .status(ReceiptStatus.PROCESSED).build());

        mockMvc.perform(delete("/api/v1/receipts/" + receiptA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 22: Cross-user PROCESS returns 403 Forbidden")
    void test22_CrossUserProcess_Returns403() throws Exception {
        Receipt receiptA = receiptRepository.save(Receipt.builder()
                .user(userA).fileName("receiptA.jpg").contentType("image/jpeg")
                .storageKey(UUID.randomUUID().toString()).fileSize(100L)
                .status(ReceiptStatus.UPLOADED).build());

        mockMvc.perform(post("/api/v1/receipts/" + receiptA.getId() + "/process")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 23: Cross-user CONFIRM returns 403 Forbidden")
    void test23_CrossUserConfirm_Returns403() throws Exception {
        Receipt receiptA = receiptRepository.save(Receipt.builder()
                .user(userA).fileName("receiptA.jpg").contentType("image/jpeg")
                .storageKey(UUID.randomUUID().toString()).fileSize(100L)
                .status(ReceiptStatus.PROCESSED).build());

        ReceiptConfirmationRequest confirmReq = ReceiptConfirmationRequest.builder()
                .merchantName("Hack Store")
                .transactionDate(LocalDate.now())
                .amount(new BigDecimal("999.00"))
                .build();

        mockMvc.perform(put("/api/v1/receipts/" + receiptA.getId() + "/confirm")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 24: No JWT returns 401 Unauthorized")
    void test24_NoJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/receipts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TEST 25: Client userId spoofing is ignored")
    void test25_ClientUserIdSpoofingIgnored() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "spoof.jpg", "image/jpeg", "Spoof User Data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .param("userId", userB.getId().toString())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());

        assertThat(receiptRepository.findAllByUserId(userA.getId(), null).getContent()).hasSize(1);
        assertThat(receiptRepository.findAllByUserId(userB.getId(), null).getContent()).isEmpty();
    }

    @Test
    @DisplayName("TEST 26: Nonexistent receipt returns 404 Not Found")
    void test26_NonexistentReceipt_Returns404() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/receipts/" + randomId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    // --- File Security & Cleanup Tests ---

    @Test
    @DisplayName("TEST 27: Storage path cannot be controlled by uploaded filename")
    void test27_StoragePathNotControlledByFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malicious_file.jpg", "image/jpeg", "File Content".getBytes()
        );

        String response = mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String receiptId = objectMapper.readTree(response).get("id").asText();
        Receipt receipt = receiptRepository.findById(UUID.fromString(receiptId)).orElseThrow();

        assertThat(receipt.getStorageKey()).isNotEqualTo("malicious_file.jpg");
        assertThat(receipt.getStorageKey()).doesNotContain("malicious");
    }

    @Test
    @DisplayName("TEST 28: Path traversal attempt in filename rejected/handled safely")
    void test28_PathTraversalAttemptRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../../etc/passwd", "image/jpeg", "Hacker content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 29: Physical file removed after receipt deletion")
    void test29_PhysicalFileRemovedAfterDeletion() throws Exception {
        String key = storageService.storeFile(userA.getId(), "phys.jpg", "Data".getBytes());
        Receipt receipt = receiptRepository.save(Receipt.builder()
                .user(userA).fileName("phys.jpg").contentType("image/jpeg")
                .storageKey(key).fileSize(10L)
                .status(ReceiptStatus.PROCESSED).build());

        assertThat(storageService.exists(userA.getId(), key)).isTrue();

        mockMvc.perform(delete("/api/v1/receipts/" + receipt.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        assertThat(storageService.exists(userA.getId(), key)).isFalse();
    }

    @Test
    @DisplayName("TEST 30: Receipt response contains no sensitive user or filesystem paths")
    void test30_ReceiptResponseExcludesSensitiveData() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "safe.jpg", "image/jpeg", "Safe content".getBytes()
        );

        String content = mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).doesNotContain("password");
        assertThat(content).doesNotContain("passwordHash");
        assertThat(content).doesNotContain("uploads/receipts");
    }

    @Test
    @DisplayName("TEST 31: Pagination works for GET /api/v1/receipts")
    void test31_PaginationWorks() throws Exception {
        receiptRepository.save(Receipt.builder().user(userA).fileName("r1.jpg").contentType("image/jpeg").storageKey("k1").fileSize(10L).status(ReceiptStatus.PROCESSED).build());
        receiptRepository.save(Receipt.builder().user(userA).fileName("r2.jpg").contentType("image/jpeg").storageKey("k2").fileSize(10L).status(ReceiptStatus.PROCESSED).build());

        mockMvc.perform(get("/api/v1/receipts?page=0&size=1")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("TEST 32: OCR failure sets FAILED status")
    void test32_OcrFailureSetsFailedStatus() throws Exception {
        MockMultipartFile corruptFile = new MockMultipartFile(
                "file", "corrupt.jpg", "image/jpeg", "FAILED_OCR corrupt data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(corruptFile)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FAILED")));
    }

    @Test
    @DisplayName("TEST 33: Failed OCR does not create transaction")
    void test33_FailedOcr_DoesNotCreateTransaction() throws Exception {
        MockMultipartFile corruptFile = new MockMultipartFile(
                "file", "corrupt.jpg", "image/jpeg", "FAILED_OCR corrupt data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(corruptFile)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());

        assertThat(transactionRepository.findAllByUserId(userA.getId())).isEmpty();
    }

    @Test
    @DisplayName("TEST 34: Confirmed OCR still does not automatically create transaction")
    void test34_ConfirmedOcr_DoesNotCreateTransaction() throws Exception {
        Receipt receipt = receiptRepository.save(Receipt.builder()
                .user(userA).fileName("test.jpg").contentType("image/jpeg")
                .storageKey(UUID.randomUUID().toString()).fileSize(100L)
                .status(ReceiptStatus.PROCESSED).build());

        ReceiptConfirmationRequest confirmReq = ReceiptConfirmationRequest.builder()
                .merchantName("Confirmed Store")
                .transactionDate(LocalDate.of(2026, 9, 7))
                .amount(new BigDecimal("1250.00"))
                .build();

        mockMvc.perform(put("/api/v1/receipts/" + receipt.getId() + "/confirm")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // Transaction table remains empty!
        assertThat(transactionRepository.findAllByUserId(userA.getId())).isEmpty();
    }

    @Test
    @DisplayName("TEST 35: Complete upload -> OCR -> confirm workflow followed by explicit transaction creation")
    void test35_CompleteUploadOcrConfirmWorkflow() throws Exception {
        // Step 1: Upload receipt
        MockMultipartFile file = new MockMultipartFile(
                "file", "supermarket.jpg", "image/jpeg", "Receipt Data".getBytes()
        );

        String uploadResponse = mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PROCESSED")))
                .andExpect(jsonPath("$.extractedAmount", is(1250.00)))
                .andReturn().getResponse().getContentAsString();

        String receiptId = objectMapper.readTree(uploadResponse).get("id").asText();

        // Step 2: Confirm OCR extracted data
        ReceiptConfirmationRequest confirmReq = ReceiptConfirmationRequest.builder()
                .merchantName("Supermarket Mega Store")
                .transactionDate(LocalDate.of(2026, 9, 7))
                .amount(new BigDecimal("1250.00"))
                .build();

        mockMvc.perform(put("/api/v1/receipts/" + receiptId + "/confirm")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // Verify no transaction created automatically
        assertThat(transactionRepository.findAllByUserId(userA.getId())).isEmpty();

        // Step 3: Explicit transaction creation via /api/v1/transactions using confirmed values
        String txJson = String.format("""
                {
                  "accountId": "%s",
                  "categoryId": "%s",
                  "type": "EXPENSE",
                  "amount": 1250.00,
                  "transactionDate": "2026-09-07",
                  "description": "Supermarket Mega Store (Receipt %s)"
                }
                """, accountA.getId(), categoryFoodA.getId(), receiptId);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(1250.00)))
                .andExpect(jsonPath("$.type", is("EXPENSE")));

        assertThat(transactionRepository.findAllByUserId(userA.getId())).hasSize(1);
    }
}
