package com.expenseguard.sync.entity;

import com.expenseguard.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * SyncOperation JPA entity tracking mobile offline synchronization requests and status.
 */
@Entity
@Table(name = "sync_operations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sync_operations_user_client_op", columnNames = {"user_id", "client_operation_id"})
}, indexes = {
        @Index(name = "idx_sync_ops_user_id", columnList = "user_id"),
        @Index(name = "idx_sync_ops_status", columnList = "status"),
        @Index(name = "idx_sync_ops_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "client_operation_id", nullable = false, length = 100)
    private String clientOperationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    private SyncOperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private SyncEntityType entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SyncOperationStatus status;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncOperation that = (SyncOperation) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
