-- ==============================================================================
-- OS4All Migration V4: AI Intelligence Layer Audit & Inference Logs (MySQL)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS ai_inference_logs (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    workflow_state VARCHAR(50) NOT NULL,
    anonymized_prompt TEXT NOT NULL,
    raw_response TEXT,
    structured_summary TEXT,
    urgency VARCHAR(50),
    confidence DECIMAL(4, 3),
    execution_time_ms BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_ai_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_ai_logs_user_id (user_id),
    INDEX idx_ai_logs_created_at (created_at DESC)
);
