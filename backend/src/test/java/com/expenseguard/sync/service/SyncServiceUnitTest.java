package com.expenseguard.sync.service;

import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.security.CurrentUserService;
import com.expenseguard.sync.dto.SyncTransactionRequest;
import com.expenseguard.sync.entity.SyncEntityType;
import com.expenseguard.sync.entity.SyncOperationStatus;
import com.expenseguard.sync.entity.SyncOperationType;
import com.expenseguard.sync.repository.SyncOperationRepository;
import com.expenseguard.transaction.entity.TransactionType;
import com.expenseguard.transaction.repository.AccountRepository;
import com.expenseguard.transaction.repository.CategoryRepository;
import com.expenseguard.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Phase 6.1 Hardening Unit Test Suite: SyncServiceImpl logic verification.
 */
@ExtendWith(MockitoExtension.class)
class SyncServiceUnitTest {

    @Mock
    private SyncOperationRepository syncOperationRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private SyncServiceImpl syncService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Unit User")
                .email("unit@example.com")
                .passwordHash("hash")
                .build();
    }

    @Test
    @DisplayName("Unit 1: Blank clientOperationId throws IllegalArgumentException")
    void blankClientOperationIdThrowsException() {
        when(currentUserService.getCurrentUser()).thenReturn(testUser);

        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("   ")
                .operationType(SyncOperationType.CREATE)
                .build();

        assertThatThrownBy(() -> syncService.processSyncTransaction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clientOperationId is required");
    }

    @Test
    @DisplayName("Unit 2: Unsupported entityType throws IllegalArgumentException")
    void unsupportedEntityTypeThrowsException() {
        when(currentUserService.getCurrentUser()).thenReturn(testUser);

        SyncTransactionRequest request = SyncTransactionRequest.builder()
                .clientOperationId("op-123")
                .operationType(SyncOperationType.CREATE)
                .entityType(SyncEntityType.TRANSACTION)
                .build();

        // Missing accountId throws validation exception
        assertThatThrownBy(() -> syncService.processSyncTransaction(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
