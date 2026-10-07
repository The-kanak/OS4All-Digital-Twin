-- ==============================================================================
-- OS4All Migration V1: Core Authentication, User Profile, Consent & Audit Schema (MySQL)
-- ==============================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BINARY(16) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_users_email (email)
);

-- 2. User Profiles Table
CREATE TABLE IF NOT EXISTS user_profiles (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL UNIQUE,
    date_of_birth DATE,
    biological_sex VARCHAR(20),
    height_cm DECIMAL(5, 2),
    weight_kg DECIMAL(5, 2),
    blood_type VARCHAR(10),
    lifestyle_notes TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_profiles_user_id (user_id)
);

-- 3. User Consents Table
CREATE TABLE IF NOT EXISTS user_consents (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    consent_type VARCHAR(50) NOT NULL,
    is_granted BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(45),
    granted_at DATETIME(6),
    revoked_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_user_consent_type UNIQUE (user_id, consent_type),
    CONSTRAINT fk_user_consents_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_consents_user_id (user_id)
);

-- 4. Audit Logs Table (Tamper-Evident Hash Chaining)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16),
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255),
    ip_address VARCHAR(45),
    details TEXT,
    previous_hash VARCHAR(64),
    integrity_hash VARCHAR(64) NOT NULL,
    logged_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_logs_user_id (user_id),
    INDEX idx_audit_logs_action (action),
    INDEX idx_audit_logs_logged_at (logged_at DESC)
);
