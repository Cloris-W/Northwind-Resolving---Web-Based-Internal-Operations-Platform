# Northwind Resolve operational schema

The PostgreSQL schema is authored only in `database/migrations/`. Flyway packages those canonical migrations into the backend build; no second handwritten migration copy is maintained.

Resolve stores a canonical operational context. It does not replace legacy systems as their source of record.

## Tables

- `cases` and append-only `case_events` hold canonical case context and timeline.
- `meter_readings` stores a time-ordered operational copy for future risk features.
- `billing_exceptions` and `bill_corrections` hold billing-quality workflow state and feedback.
- `field_visits` holds the minimum operational field-work record.
- `ai_analysis` stores validated, traceable advisory summary or recommendation output metadata.
- `audit_events` stores hashes and future chain-submission state; no customer PII or complaint content is stored on-chain.

The initial migration creates exactly the eight operational tables above. Migration V2 adds read-only imported-source tables: `legacy_systems`, `legacy_complaints`, `monthly_kpis`, `meter_region_monthly_metrics`, `ai_pilot_monthly_metrics`, and `unit_costs`. These preserve the supplied CSV facts without treating aggregate source data as account-level records.

`legacy_complaints.complaint_id` is projected directly into `cases.case_id`; both are `VARCHAR(64)`. The raw complaint status, category, priority, and source-system identifier remain preserved in `legacy_complaints`, while the case row carries the canonical normalized values and `source_system_id`.

## Deterministic complaint normalization

| CSV field | Raw value | Canonical value |
| --- | --- | --- |
| `priority` | `P1` | `CRITICAL` |
| `priority` | `P2` | `HIGH` |
| `priority` | `P3` | `MEDIUM` |
| `category` | `Billing - disputed amount`, `Billing - estimated read`, `Payment - plan or arrears` | `BILLING` |
| `category` | `Metering - no read taken` | `METERING` |
| `category` | `Service - missed appointment`, `Service - poor communication`, `Supply - interruption`, `Water - pressure or quality` | `SERVICE` |
| `category` | `Other` | `OTHER` |
| `status` | `Open` | `OPEN` |
| `status` | `Closed` | `CLOSED` |
| `status` | `Closed - reopened` | `IN_PROGRESS` |

Any unlisted raw value fails the import rather than being inferred. Each source file is parsed and header-validated before its own transaction begins; natural-key upserts make repeat imports stable. Complaint-created events use a deterministic UUID, so one complaint yields one `COMPLAINT_CREATED` event.

## Phase 3 Case Workspace read limits

The Case Workspace reads canonical `cases` and `case_events`. It may also read canonical `meter_readings`, `bill_corrections`, `billing_exceptions`, and persisted `field_visits` when records exist. Phase 2 does not populate those account-level operational tables from aggregate or incomplete CSV data:

- `meter_region_monthly_metrics` is regional/monthly aggregate data and is never returned as account meter readings.
- `bill_correction_value` in `legacy_complaints` is not a canonical invoice or correction record.
- `field_visits` remains empty until the later FieldForce workflow persists a visit.

Consequently, valid account endpoints return truthful empty arrays where the supplied source does not provide contract-compatible records.

## Phase 4 mutation integrity

Migration V3 adds `mutation_idempotency`, which durably records the response and request fingerprint for the contract-required idempotency key on case transfers and field-visit requests. A transaction-scoped advisory lock serializes each operation/key pair. Replaying the same request returns the stored result; reusing a key with different input is rejected as a state conflict.

Case transfer updates only `cases.assigned_team` and appends a `TRANSFERRED` event in the same transaction. A FieldForce request is sent through the mock provider with only case ID, region, schedule, reason, and optional meter/instructions; it persists one `field_visits` row and appends one `FIELD_VISIT_REQUESTED` event atomically. The approved API contract has no result-writeback operation, so no completion workflow or `FIELD_VISIT_COMPLETED` event is exposed yet.

## Phase 5 billing quality

`billing_exceptions.region` is a billing-owned, non-null snapshot column established in V1. Phase 5 exception generation copies the evaluated case region into it; queue filtering reads only this table. The MVP creates at most one generated exception per source case and never reopens or overwrites an existing reviewed exception on a later startup scan.

The supplied source has no account-level estimated-read or meter-history facts. Those risk features remain unavailable rather than inferred from complaint categories or regional aggregates. Legacy correction evidence is boolean-only and must precede the evaluated complaint; canonical correction feedback is only earlier `bill_corrections.created_at` records.
