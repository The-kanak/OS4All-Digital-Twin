-- ==============================================================================
-- OS4All Migration V3: Health Report Ingestion and OCR Extraction Pipeline (MySQL)
-- ==============================================================================

-- 1. Extend lab_reports with file storage, hash, OCR status, and extracted text
ALTER TABLE lab_reports ADD COLUMN file_name VARCHAR(255);
ALTER TABLE lab_reports ADD COLUMN file_path VARCHAR(500);
ALTER TABLE lab_reports ADD COLUMN mime_type VARCHAR(100);
ALTER TABLE lab_reports ADD COLUMN file_size BIGINT;
ALTER TABLE lab_reports ADD COLUMN file_hash VARCHAR(64);
ALTER TABLE lab_reports ADD COLUMN ocr_status VARCHAR(50) DEFAULT 'COMPLETED';
ALTER TABLE lab_reports ADD COLUMN raw_extracted_text TEXT;
ALTER TABLE lab_reports ADD COLUMN overall_confidence DECIMAL(4, 3) DEFAULT 1.000;
ALTER TABLE lab_reports ADD COLUMN review_required BOOLEAN DEFAULT FALSE;

CREATE INDEX idx_lab_reports_ocr_status ON lab_reports(ocr_status);
CREATE INDEX idx_lab_reports_review_required ON lab_reports(review_required);

-- 2. Extend lab_results with extracted value, confidence, source text, review flag, and confirmation status
ALTER TABLE lab_results ADD COLUMN extracted_value VARCHAR(100);
ALTER TABLE lab_results ADD COLUMN confidence DECIMAL(4, 3) DEFAULT 1.000;
ALTER TABLE lab_results ADD COLUMN source_text TEXT;
ALTER TABLE lab_results ADD COLUMN review_required BOOLEAN DEFAULT FALSE;
ALTER TABLE lab_results ADD COLUMN is_confirmed BOOLEAN DEFAULT TRUE;

CREATE INDEX idx_lab_results_review_required ON lab_results(review_required);
