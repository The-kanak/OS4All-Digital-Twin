-- ==============================================================================
-- OS4All Migration V2: Health Data Engine Normalized Observations & Labs Schema (MySQL)
-- ==============================================================================

-- 1. Base Health Observations Table (Single-Table / Joined Polymorphic Hierarchy)
CREATE TABLE IF NOT EXISTS health_observations (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    observation_type VARCHAR(50) NOT NULL,
    dtype VARCHAR(50) NOT NULL,
    value_numeric DECIMAL(12, 4),
    value_text TEXT,
    unit VARCHAR(50),
    standard_value_numeric DECIMAL(12, 4),
    standard_unit VARCHAR(50),
    timestamp DATETIME(6) NOT NULL,
    source VARCHAR(100) NOT NULL,
    confidence DECIMAL(4, 3) NOT NULL DEFAULT 1.000,
    metadata TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    -- Vital Measurement specific fields
    vital_name VARCHAR(100),
    device_model VARCHAR(100),

    -- Lifestyle Observation specific fields
    lifestyle_category VARCHAR(100),
    duration_minutes INTEGER,

    -- Symptom Observation specific fields
    symptom_name VARCHAR(100),
    severity VARCHAR(50),
    body_site VARCHAR(100),

    CONSTRAINT fk_health_obs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_health_obs_user_id (user_id),
    INDEX idx_health_obs_type (observation_type),
    INDEX idx_health_obs_timestamp (timestamp DESC),
    INDEX idx_health_obs_user_timestamp (user_id, timestamp DESC),
    INDEX idx_health_obs_user_type (user_id, observation_type)
);

-- 2. Lab Reports Table
CREATE TABLE IF NOT EXISTS lab_reports (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    report_title VARCHAR(255) NOT NULL,
    laboratory_name VARCHAR(255),
    collection_date DATETIME(6) NOT NULL,
    reported_date DATETIME(6),
    source VARCHAR(100) NOT NULL,
    notes TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_lab_reports_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_lab_reports_user_id (user_id),
    INDEX idx_lab_reports_collection_date (collection_date DESC)
);

-- 3. Lab Results Table
CREATE TABLE IF NOT EXISTS lab_results (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    lab_report_id BINARY(16),
    biomarker VARCHAR(100) NOT NULL,
    standardized_biomarker VARCHAR(100) NOT NULL,
    `value` DECIMAL(12, 4) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    standard_value DECIMAL(12, 4),
    standard_unit VARCHAR(50),
    reference_low DECIMAL(12, 4),
    reference_high DECIMAL(12, 4),
    collection_date DATETIME(6) NOT NULL,
    source_report VARCHAR(255),
    notes TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_lab_results_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_results_report FOREIGN KEY (lab_report_id) REFERENCES lab_reports(id) ON DELETE CASCADE,
    INDEX idx_lab_results_user_id (user_id),
    INDEX idx_lab_results_report_id (lab_report_id),
    INDEX idx_lab_results_biomarker (standardized_biomarker),
    INDEX idx_lab_results_collection_date (collection_date DESC)
);
