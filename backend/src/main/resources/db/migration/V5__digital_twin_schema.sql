-- ==============================================================================
-- OS4All Digital Twin Migration V5: Digital Twin, Risk Predictions & Simulations
-- ==============================================================================

-- 1. Historical Medical Records Table
CREATE TABLE IF NOT EXISTS historical_medical_records (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    record_type VARCHAR(100) NOT NULL,
    condition_or_diagnosis VARCHAR(255) NOT NULL,
    icd10_code VARCHAR(20),
    severity VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    diagnosed_date DATE,
    medications TEXT,
    family_history_notes TEXT,
    clinical_notes TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_hist_med_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_hist_med_user_id (user_id),
    INDEX idx_hist_med_type (record_type)
);

-- 2. Digital Twin States Table
CREATE TABLE IF NOT EXISTS digital_twin_states (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    twin_state VARCHAR(50) NOT NULL, -- STABLE, PRE_SYMPTOMATIC_DRIFT, ELEVATED_RISK, ACTIVE_ANOMALY
    overall_risk_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    metabolic_risk_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    glucose_spike_probability DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    prediction_horizon VARCHAR(50) DEFAULT 'Next 2 Hours',
    confidence DECIMAL(4, 3) NOT NULL DEFAULT 0.900,
    state_drivers TEXT,
    physiological_state_summary TEXT,
    last_simulation_scenario VARCHAR(100),
    last_updated_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_twin_state_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_twin_state_user_id (user_id),
    INDEX idx_twin_state_updated (last_updated_at DESC)
);

-- 3. Digital Twin Predictions Table
CREATE TABLE IF NOT EXISTS digital_twin_predictions (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    prediction_type VARCHAR(100) NOT NULL, -- e.g. GLUCOSE_SPIKE_2H, METABOLIC_DRIFT_24H
    horizon_window VARCHAR(50) NOT NULL, -- e.g. "Next 2 Hours"
    risk_level VARCHAR(50) NOT NULL,     -- LOW, MODERATE, HIGH, CRITICAL
    probability DECIMAL(5, 2) NOT NULL,  -- 0.00 to 100.00
    model_name VARCHAR(100) NOT NULL,
    headline VARCHAR(255) NOT NULL,
    clinical_explanation TEXT NOT NULL,
    top_contributing_factors TEXT,
    historical_evidence TEXT,
    recommended_clinical_actions TEXT,
    confidence DECIMAL(4, 3) NOT NULL,
    is_simulation BOOLEAN NOT NULL DEFAULT FALSE,
    disclaimer VARCHAR(255) NOT NULL DEFAULT 'Research / Hackathon Prototype — Not a Medical Diagnosis.',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_twin_pred_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_twin_pred_user_id (user_id),
    INDEX idx_twin_pred_created_at (created_at DESC)
);
