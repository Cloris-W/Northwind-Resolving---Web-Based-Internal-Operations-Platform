# Northwind Resolve — Codex Instructions

## Source of Truth
Before implementing or modifying major functionality, read:
- `docs/IMPLEMENTATION_SPEC.md`

Treat that document as the authoritative product and architecture specification.

## Architecture
- Frontend: Angular + TypeScript
- Backend: Kotlin + Spring Boot
- Architecture style: Modular Monolith
- APIs must be contract-first using OpenAPI.
- Use Controller -> Service -> Repository/Adapter separation.
- Do not introduce microservices unless explicitly requested.
- Do not let frontend code access the database directly.

## Domain Boundaries
Keep these domains separated:
- Case Management
- Billing Quality
- Metering
- FieldForce Integration
- AI Assistance
- Audit
- Analytics / Value

Do not directly access another module's persistence layer.
Communicate through defined interfaces/services.

## Legacy Integration
Northwind legacy systems are fictional.
Use the provided CSV files and mock adapters to simulate:
- CaseTrack
- MeterHub
- Billing
- FieldForce
- CallCentre

Do not claim or implement real Northwind connectivity.

## Data Rules
- Preserve a stable Case ID across transfers.
- Case history must be append-only through case events.
- Legacy systems remain authoritative for their own domains.
- Northwind Resolve provides a canonical operational view, not a replacement master database.
- Never put customer PII or complaint content on Solana.

## Gemini Rules
Gemini may:
- summarize
- classify
- recommend
- explain
- draft responses

Gemini must not:
- calculate deterministic billing anomalies
- change bills
- approve refunds or compensation
- close complaints
- override meter readings

All consequential actions require deterministic business rules and/or human approval.

## Billing Quality Rules
Billing anomaly detection must use deterministic logic.
Corrections must create feedback records.
High-risk cases must enter an exception workflow rather than being silently auto-corrected.

## Testing
For every business-rule change:
- add or update unit tests
- add integration tests where persistence or APIs are affected
- run relevant tests before finishing

Important failure cases:
- Gemini unavailable -> core workflow must still work
- Solana unavailable -> resolution must still work; audit may remain pending
- Transfers must preserve full case history

## Security
- Never commit API keys, passwords, `.env`, private keys, or secrets.
- Validate authorization on the backend.
- Do not rely on frontend validation for security.
- Avoid logging sensitive customer data.

## Development Workflow
- Implement one specification phase at a time unless explicitly asked otherwise.
- Inspect existing code before creating new abstractions.
- Prefer the simplest implementation that satisfies the specification.
- Do not add frameworks, infrastructure, or dependencies without a clear need.
- Keep changes scoped to the requested phase/task.
- Run tests after code changes.

## Definition of Done
A feature is complete only when:
- implementation is finished
- relevant tests pass
- API contract is updated when applicable
- documentation is updated when architecture or behavior changes
- no secrets are committed