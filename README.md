# Northwind Resolve

Northwind Resolve is an internal operations platform built as an Angular frontend and a Kotlin/Spring Boot modular monolith. This repository currently contains Phase 0 only: a runnable application scaffold and local PostgreSQL container configuration. No business workflows, database schema, API contract, legacy adapters, Gemini, Solana, Tiger Data, CI/CD, or deployment integration have been implemented.

## Prerequisites

- Node.js 20.19+, 22.12+, or 24+
- npm 10+
- Java 21
- Docker Desktop (optional, for the local PostgreSQL container)

## Start locally

1. Create local environment settings from `.env.example` if you want to change PostgreSQL defaults:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Install and start the frontend:

   ```powershell
   Set-Location frontend
   npm install
   npm start
   ```

   Open `http://localhost:4200`.

3. Start the backend in another terminal:

   ```powershell
   Set-Location backend
   .\gradlew.bat bootRun
   ```

   Confirm health at `http://localhost:8080/actuator/health`.

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

## Repository layout

- `frontend/` — Angular + TypeScript user interface.
- `backend/` — Kotlin + Spring Boot modular monolith.
- `data/` — six original synthetic challenge CSV inputs; not imported in Phase 0.
- `docs/` — architecture, contract, schema, testing, and deployment documentation.

## Phase 0 limitations

The backend exposes only Spring Boot Actuator health/info endpoints. The frontend is a static placeholder. Phase 1 will define the OpenAPI contract, error schema, migrations, and core DTOs/entities.
