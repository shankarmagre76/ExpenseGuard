-- Migration for Phase 5.3: Sync Operations & Optimistic Versioning

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS sync_operations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    client_operation_id VARCHAR(100) NOT NULL,
    operation_type VARCHAR(30) NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id UUID,
    status VARCHAR(30) NOT NULL,
    payload_hash VARCHAR(64),
    error_code VARCHAR(100),
    error_message VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_sync_operations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_sync_operations_user_client_op UNIQUE (user_id, client_operation_id)
);

CREATE INDEX IF NOT EXISTS idx_sync_ops_user_id ON sync_operations (user_id);
CREATE INDEX IF NOT EXISTS idx_sync_ops_status ON sync_operations (status);
CREATE INDEX IF NOT EXISTS idx_sync_ops_created_at ON sync_operations (created_at);
