CREATE TABLE cases (
    case_id VARCHAR(64) PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    category VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    region VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    sla_days INTEGER NOT NULL CHECK (sla_days >= 0),
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    assigned_team VARCHAR(100) NOT NULL
);

CREATE TABLE case_events (
    event_id UUID PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL REFERENCES cases(case_id),
    event_type VARCHAR(64) NOT NULL,
    source_system VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor VARCHAR(128) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    metadata_json JSONB
);

CREATE TABLE meter_readings (
    id UUID PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    meter_id VARCHAR(64) NOT NULL,
    reading_at TIMESTAMPTZ NOT NULL,
    value NUMERIC(18, 4) NOT NULL CHECK (value >= 0),
    estimated_flag BOOLEAN NOT NULL,
    region VARCHAR(64) NOT NULL,
    UNIQUE (account_id, meter_id, reading_at)
);

CREATE TABLE billing_exceptions (
    id UUID PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) REFERENCES cases(case_id),
    risk_score INTEGER NOT NULL CHECK (risk_score BETWEEN 0 AND 100),
    risk_level VARCHAR(16) NOT NULL,
    reason_codes JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    region VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    reviewed_by VARCHAR(128)
);

CREATE TABLE bill_corrections (
    id UUID PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    original_value NUMERIC(18, 4) NOT NULL,
    corrected_value NUMERIC(18, 4) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    region VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE field_visits (
    id UUID PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL REFERENCES cases(case_id),
    status VARCHAR(32) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    outcome VARCHAR(1000)
);

CREATE TABLE ai_analysis (
    id UUID PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL REFERENCES cases(case_id),
    analysis_type VARCHAR(32) NOT NULL,
    input_hash VARCHAR(64) NOT NULL,
    model VARCHAR(128) NOT NULL,
    output_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL REFERENCES cases(case_id),
    event_type VARCHAR(64) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    solana_signature VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_cases_account_id ON cases(account_id);
CREATE INDEX idx_case_events_case_id_occurred_at ON case_events(case_id, occurred_at);
CREATE INDEX idx_meter_readings_account_id_reading_at ON meter_readings(account_id, reading_at);
CREATE INDEX idx_billing_exceptions_status_risk_region ON billing_exceptions(status, risk_level, region);
CREATE INDEX idx_bill_corrections_account_id_created_at ON bill_corrections(account_id, created_at);
CREATE INDEX idx_field_visits_case_id_status ON field_visits(case_id, status);
CREATE INDEX idx_ai_analysis_case_id_created_at ON ai_analysis(case_id, created_at);
CREATE INDEX idx_audit_events_case_id_status ON audit_events(case_id, status);
