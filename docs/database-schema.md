# Northwind Resolve operational schema

The PostgreSQL schema is authored only in `database/migrations/`. Flyway packages those canonical migrations into the backend build; no second handwritten migration copy is maintained.

Resolve stores a canonical operational context. It does not replace legacy systems as their source of record.

## Tables

- `cases` and append-only `case_events` hold canonical case context and timeline.
- `meter_readings` stores a time-ordered operational copy for future risk features.
- `billing_exceptions` and `bill_corrections` hold billing-quality workflow state and feedback.
- `field_visits` holds the minimum operational field-work record.
- `ai_analysis` stores traceable future advisory output metadata.
- `audit_events` stores hashes and future chain-submission state; no customer PII or complaint content is stored on-chain.

The initial migration has no seed data and creates exactly these eight operational tables.
