# Northwind Resolve

Northwind Resolve is an internal operations platform built as an Angular frontend and a Kotlin/Spring Boot modular monolith. Phase 3 provides a Case Workspace for canonical case search, case context, timeline, and truthful available account context from the Phase 2 imported data.

## Prerequisites

- Node.js 20.19+, 22.12+, or 24+
- npm 10+
- Java 21
- Docker Desktop (optional, for the local PostgreSQL container)

## Start locally

1. Create local environment settings. `.env.local` is ignored by Git and persists settings across shells:

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

The Case Workspace does not fabricate missing source facts. The supplied meter data is regional/monthly aggregate data, so account meter histories are empty. Detailed invoices and canonical bill corrections are not supplied, so billing histories may be empty. FieldForce visits remain empty until a later workflow creates them. Transfer, field-visit creation/results, billing risk/review, Gemini, Solana, dashboard, and value-case workflows remain deferred.
