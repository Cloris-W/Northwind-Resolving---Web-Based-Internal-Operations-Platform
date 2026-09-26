ALTER TABLE cases
    ADD COLUMN source_system_id VARCHAR(64);

CREATE TABLE legacy_systems (
    system_id VARCHAR(64) PRIMARY KEY,
    system_name VARCHAR(255) NOT NULL,
    purpose TEXT NOT NULL,
    year_installed INTEGER NOT NULL,
    vendor VARCHAR(255) NOT NULL,
    tech_stack TEXT NOT NULL,
    records_held TEXT NOT NULL,
    integration_method VARCHAR(255) NOT NULL,
    annual_run_cost NUMERIC(14, 2) NOT NULL,
    owning_function VARCHAR(255) NOT NULL,
    notes TEXT NOT NULL
);

CREATE TABLE legacy_complaints (
    complaint_id VARCHAR(64) PRIMARY KEY,
    date_opened DATE NOT NULL,
    date_closed DATE,
    raw_status VARCHAR(128) NOT NULL,
    channel VARCHAR(128) NOT NULL,
    raw_category VARCHAR(255) NOT NULL,
    raw_priority VARCHAR(32) NOT NULL,
    region VARCHAR(128) NOT NULL,
    source_system_id VARCHAR(64) NOT NULL,
    transferred_between_systems BOOLEAN NOT NULL,
    sla_days INTEGER NOT NULL,
    days_to_close INTEGER,
    sla_breach BOOLEAN NOT NULL,
    reopened BOOLEAN NOT NULL,
    resolution_action TEXT,
    resolvable_by_information_only BOOLEAN,
    bill_correction_value NUMERIC(14, 2),
    account_id VARCHAR(64) NOT NULL
);

CREATE TABLE monthly_kpis (
    month DATE PRIMARY KEY,
    complaints_opened INTEGER NOT NULL,
    complaints_closed INTEGER NOT NULL,
    avg_days_to_close NUMERIC(10, 2) NOT NULL,
    first_contact_resolution_rate NUMERIC(8, 5) NOT NULL,
    inbound_calls INTEGER NOT NULL,
    cost_to_serve_per_account NUMERIC(12, 2) NOT NULL,
    regulator_satisfaction_score_of_5 NUMERIC(5, 2) NOT NULL
);

CREATE TABLE meter_region_monthly_metrics (
    month DATE NOT NULL,
    region VARCHAR(128) NOT NULL,
    accounts INTEGER NOT NULL,
    estimated_read_rate NUMERIC(8, 5) NOT NULL,
    smart_meter_penetration NUMERIC(8, 5) NOT NULL,
    billing_exceptions_raised INTEGER NOT NULL,
    systems_serving_region TEXT NOT NULL,
    PRIMARY KEY (month, region)
);

CREATE TABLE ai_pilot_monthly_metrics (
    month DATE PRIMARY KEY,
    assistant_sessions INTEGER NOT NULL,
    fully_contained_rate NUMERIC(8, 5) NOT NULL,
    escalated_to_agent_rate NUMERIC(8, 5) NOT NULL,
    abandoned_rate NUMERIC(8, 5) NOT NULL,
    repeat_contact_within_7_days_rate NUMERIC(8, 5) NOT NULL,
    assistant_csat_of_5 NUMERIC(5, 2) NOT NULL,
    complaint_raised_after_session_rate NUMERIC(8, 5) NOT NULL
);

CREATE TABLE unit_costs (
    item VARCHAR(255) PRIMARY KEY,
    unit_cost NUMERIC(14, 2) NOT NULL,
    unit VARCHAR(128) NOT NULL,
    source_note TEXT NOT NULL
);

CREATE INDEX idx_legacy_complaints_account_id ON legacy_complaints (account_id);
CREATE INDEX idx_legacy_complaints_source_system_id ON legacy_complaints (source_system_id);
CREATE INDEX idx_cases_source_system_id ON cases (source_system_id);
