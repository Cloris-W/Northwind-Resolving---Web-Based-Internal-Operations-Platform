ALTER TABLE audit_events
    ADD COLUMN source_event_reference VARCHAR(128);

CREATE UNIQUE INDEX uq_audit_events_source_reference
    ON audit_events (event_type, source_event_reference)
    WHERE source_event_reference IS NOT NULL;
