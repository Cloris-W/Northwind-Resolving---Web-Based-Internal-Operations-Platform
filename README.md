# Northwind Resolve

Northwind Resolve is an internal operations platform built as an Angular frontend and a Kotlin/Spring Boot modular monolith. Phase 4 adds idempotent case transfer and FieldForce visit requests to the Case Workspace while retaining the same Case ID and append-only timeline.

## Prerequisites

- Node.js 20.19+, 22.12+, or 24+
- npm 10+
- Java 21
- Docker Desktop (optional, for the local PostgreSQL container)

## Start locally

1. Optionally create local environment settings. `.env.local` is ignored by Git and persists settings across shells. It is useful when the local PostgreSQL port differs from `5432` (for example `5433`):

   ```powershell
   Copy-Item .env.example .env.local
   ```

2. Install and start the frontend:

   ```powershell
   Set-Location frontend
   npm install
   npm start
   ```

   Open `http://localhost:4200`.

3. Start PostgreSQL and the backend with the reusable local script:

   ```powershell
   .\scripts\run-local.ps1
   ```

   The backend uses the `database` profile and listens on `http://localhost:18080`.
   Confirm health at `http://localhost:18080/actuator/health`.

   View the canonical API contract in Swagger UI at `http://localhost:18080/swagger-ui/index.html`.

4. Optionally start local PostgreSQL (not yet connected to the application):

   ```powershell
   docker compose up -d database
   ```

## Build and test

```powershell
Set-Location frontend
npm test
npm run build

Set-Location ../backend
.\gradlew.bat test
.\gradlew.bat build
```

Run the mandatory full phase gate after completing a phase. It starts Docker services, requires all backend tests (including Testcontainers) to execute without skips, builds both applications, checks backend endpoints, and verifies Git hygiene.

```powershell
.\scripts\verify-phase.ps1
```

## Repository layout

- `frontend/` — Angular + TypeScript user interface.
- `backend/` — Kotlin + Spring Boot modular monolith.
- `data/` — the single source location for six original synthetic challenge CSV inputs.
- `database/migrations/` — canonical versioned PostgreSQL migrations, including imported-source tables.
- `docs/` — architecture, contract, schema, testing, and deployment documentation.

## Case Workspace limitations

The Case Workspace does not fabricate missing source facts. The supplied meter data is regional/monthly aggregate data, so account meter histories are empty. Detailed invoices are not supplied. Billing Quality reviews may create canonical correction feedback, but source-data risk evaluation does not infer account meter facts from complaint categories or regional aggregates. FieldForce visits begin only when a user requests one through the approved API. The approved contract does not yet include a FieldForce result-writeback endpoint, so visit completion/results remain deferred.

## Gemini assistance

Gemini summary and recommendation requests are advisory-only and run through the backend. Configure an optional `GEMINI_API_KEY` in a local ignored `.env.local`; without it, the application starts normally and AI requests return a temporary-unavailable response. AI output is structured and validated before it is displayed or persisted. It cannot perform transfers, request field visits, alter billing, or send customer communication.

## Solana audit

Solana is disabled by default and targets Devnet only. Audit anchors contain only a SHA-256 hash in a Memo Program instruction; no PII, correction values, case/account identifiers, or secrets are written on-chain. A local signer path may be configured in ignored `.env.local`; pending audits are retried in the background. The live Devnet demonstration is a separate manual step.
