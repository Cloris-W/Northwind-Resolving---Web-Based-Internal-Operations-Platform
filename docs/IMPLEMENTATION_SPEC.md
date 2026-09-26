# Northwind Resolve

## Codex Implementation Specification and Development Workflow

*Web-Based Internal Operations Platform*

| **Document Purpose:** This document can be handed directly to Codex as the implementation specification. Codex should implement according to the module boundaries, API contracts, data model, development sequence, testing requirements, and Definition of Done in this document; any deviation should first be documented in the README / ADR. |
| --- |

Version 1.0 | Hackathon MVP / Extensible Architecture

# 1. Project Goals and Implementation Principles

**Northwind Resolve is an internal operations Web Application.**It does not immediately replace legacy systems such as CaseTrack, MeterHub, FieldForce, or Billing; instead, it adds an Integration Layer, Case Context Layer, Billing Quality Layer, AI Assistance, and Audit Layer on top.

| **Goal** | **Problem to Solve** | **Primary Mechanism** |
| --- | --- | --- |
| Resolve Existing Complaints Faster | Case transfer history loss, Agent switching across multiple systems, FieldForce lacking context | Canonical Case Context + Unified Workspace + Integration Adapters |
| Reduce New Complaint Creation | Estimated read / billing anomalies are detected only after bills are issued, and bill corrections have no feedback loop | Billing Validation + Exception Queue + Correction Feedback |
| Keep AI Controlled | LLMs must not make high-risk decisions directly | Gemini summarizes, classifies, recommends, and drafts only; business rules and human operators determine outcomes |
| Provide Verifiable Auditing | Critical decisions require traceability | Critical event hash -> Solana Devnet; raw customer data stays off-chain |

**Architecture decision:**API-first + Modular Monolith + Integration Adapters. The MVP does not use microservices, Kafka, Kubernetes, or a self-trained ML model.

# 2. MVP Scope and Non-Goals

## 2.1 Must-Have (MVP)

- Executive Dashboard: Backlog, SLA, FCR, transfer, reopen, billing exception, estimated-read, value metrics.
- Case Workspace: Search by Case ID / Account ID and view unified case context, timeline, billing, meter, and field visit data.
- Case Transfer: Append a TRANSFERRED event under the same Case ID; do not create a new case or lose history.
- FieldForce Mock Integration: Create a field visit, provide the minimum necessary context, and write the result back to the timeline.
- Billing Quality Monitor: deterministic risk rules, exception queue, review / correction flow.
- Correction Feedback: Store original estimate, corrected value, reason, meter/account/region/date.
- Gemini: Case summary, classification, recommended action, customer-response draft (structured output).
- Solana: Write hashes of critical events to Devnet and provide a verification UI.
- Value Case: Calculate savings, net benefit, and payback using Northwind unit costs.
- Docker + GitHub Actions + Vultr deployment + health check.
## 2.2 Explicitly Out of Scope (MVP)

- Do not connect to real Northwind systems (the challenge systems are fictional); use the six CSV files to create mock adapters / seed data.
- Do not integrate all 15 systems at once; implement only mock integrations relevant to the core complaint journey.
- Do not allow Gemini to automatically issue refunds, modify bills, approve compensation, override meter readings, or close cases.
- Do not write customer PII, full complaint text, or bill details to Solana.
- Do not perform a full smart-meter rollout, legacy replacement, or custom anomaly ML model within the MVP.
# 3. Technology Stack (Fixed Recommendation)

| **Layer** | **Technology** | **Purpose / Rules** |
| --- | --- | --- |
| Frontend | Angular + TypeScript | Dashboard, Case Workspace, Billing Quality UI; the frontend must not access the DB directly |
| UI | Angular Material + CSS | Tables / cards / dialogs / forms; prioritize clarity and functionality over visual polish |
| Backend | Spring Boot + Kotlin | REST API, domain logic, integration orchestration |
| API Contract | OpenAPI / Swagger | Define DTOs, status codes, and error formats before implementation |
| Database | Tiger Data / PostgreSQL | Case context, event timeline, meter time-series, exceptions, feedback, KPI |
| Persistence | Spring Data JPA / JDBC | Repository interface; domain services do not write SQL directly |
| AI | Gemini API | Structured output: summary / classification / recommendation / draft |
| Audit | Solana Devnet | Store only critical event hashes / tx signatures |
| Hosting | Vultr Cloud Compute | Run the app / Docker; provide a demo endpoint |
| Container | Docker + docker-compose | Keep local and deployment environments consistent |
| CI/CD | GitHub Actions | Test -> Build -> Docker -> Deploy -> Health Check |
| Testing | JUnit, MockMvc, Testcontainers, Playwright | Unit / integration / API / E2E |
| Observability | Spring Actuator + structured logs | Track Health / DB / Gemini / Solana status and errors |

# 4. System Architecture and Boundaries

```text
                 NORTHWIND RESOLVE

┌──────────────────────────────────────────┐
│ Presentation: Angular                   │
│ Dashboard | Case Workspace | Billing QA │
└──────────────────┬───────────────────────┘
                   │ REST / JSON
┌──────────────────▼───────────────────────┐
│ API / Application Layer                 │
│ Auth | Validation | Error Handling      │
└──────────────────┬───────────────────────┘
                   │
┌──────────────────▼───────────────────────┐
│ Domain Layer (Modular Monolith)         │
│ Case | Billing | Meter | AI | Audit | KPI│
└───────────────┬─────────────┬────────────┘
                │             │
        ┌───────▼───────┐ ┌───▼──────────┐
        │ Tiger Data    │ │ External APIs │
        │ PostgreSQL    │ │ Gemini/Solana │
        └───────▲───────┘ └──────────────┘
                │
┌───────────────┴──────────────────────────┐
│ Integration Adapters                    │
│ CaseTrack | MeterHub | Billing |        │
│ FieldForce | CallCentre (mock in MVP)   │
└──────────────────────────────────────────┘

Infrastructure / deployment: Vultr + Docker
```

**Core Rule:**Domain / Business Logic must not depend on Angular, HTTP controllers, specific DB drivers, or the Solana/Gemini SDK; external systems must be accessed through interfaces / adapters.

# 5. Repository and Module Structure

```text
northwind-resolve/
├── frontend/                     # Angular
├── backend/                      # Spring Boot + Kotlin
│   └── src/main/kotlin/.../
│       ├── auth/
│       ├── cases/
│       ├── billing/
│       ├── metering/
│       ├── fieldforce/
│       ├── ai/
│       ├── audit/
│       ├── analytics/
│       ├── integrations/
│       └── common/
├── database/
│   ├── migrations/
│   ├── seed/
│   └── schema/
├── data/                    # 6 challenge CSVs
├── docs/
│   ├── architecture.md
│   ├── api-contract.yaml
│   ├── database-schema.md
│   ├── testing.md
│   └── deployment.md
├── .github/workflows/
├── docker-compose.yml
├── .env.example
└── README.md
```

# 6. Data Ownership / SSOT Principles

**Do not describe Tiger Data as the single master for all Northwind data.**In a real environment, each legacy domain retains its own Source of Record; Resolve creates a Canonical Operational Context.

| **Domain** | **Real-World Source of Record** | **MVP Simulation Source** | **What Resolve Stores** |
| --- | --- | --- | --- |
| Complaint | CaseTrack | northwind_complaints.csv | Canonical case + case_events + refs |
| Meter | MeterHub / SmartRead | northwind_meter_reads.csv | Time-series copy / risk features |
| Billing | Aurora / billing systems | Complaint resolution fields + mock records | Exception / correction / references |
| Field work | FieldForce | Mock generated data | Visit context / status / event |
| KPI | Ops / reporting | northwind_monthly_kpis.csv | Aggregates / dashboard metrics |
| Unit cost | Finance | northwind_unit_costs.csv | Value-case assumptions / calculation inputs |

# 7. Core Data Model

| **Table / Entity** | **Minimum Fields** | **Purpose** |
| --- | --- | --- |
| cases | case_id, account_id, category, priority, region, status, sla_days, opened_at, closed_at, assigned_team | Canonical complaint record |
| case_events | event_id, case_id, event_type, source_system, timestamp, actor, description, metadata_json | Persistent timeline; transfer does not overwrite previous events |
| meter_readings | account_id, meter_id, timestamp/month, value/rate, estimated_flag, region | Meter history / anomaly input |
| billing_exceptions | id, account_id, case_id?, risk_score, reason_codes, status, created_at, reviewed_by | Pre-bill / anomaly review queue |
| bill_corrections | id, account_id, original_value, corrected_value, reason, region, created_at | Feedback loop dataset |
| field_visits | id, case_id, status, scheduled_at, completed_at, outcome | FieldForce workflow |
| ai_analysis | id, case_id, type, input_hash, model, output_json, created_at | Traceable AI output |
| audit_events | id, case_id, event_type, payload_hash, solana_signature, status, created_at | Tamper-evident audit |

**Event types must include at least:** COMPLAINT_CREATED, TRANSFERRED, BILL_CHECKED, METER_REVIEWED, FIELD_VISIT_REQUESTED, FIELD_VISIT_COMPLETED, BILL_CORRECTED, CASE_CLOSED.

# 8. API Contract (Define Before Implementation)

| **Method** | **Endpoint** | **Purpose** | **Key Requirements** |
| --- | --- | --- | --- |
| GET | /api/cases | Search/list cases | Filter by status/category/region/SLA/account |
| GET | /api/cases/{caseId} | Unified case context | Return canonical DTO, not raw legacy format |
| GET | /api/cases/{caseId}/timeline | Case event timeline | Chronological, append-only view |
| POST | /api/cases/{caseId}/transfer | Transfer case | Same case ID + TRANSFERRED event |
| POST | /api/cases/{caseId}/field-visit | Request field work | Only minimum necessary context |
| GET | /api/accounts/{accountId}/meter-readings | Meter history | Time-ordered data |
| GET | /api/accounts/{accountId}/billing | Account billing history | Canonical billing history and correction references |
| GET | /api/billing/exceptions | Exception queue | Filter by risk/status/region |
| GET | /api/billing/exceptions/{id} | Billing exception detail | Return review context for one exception |
| POST | /api/billing/exceptions/{id}/review | Review exception | Validated action + audit/event |
| POST | /api/ai/cases/{caseId}/summary | Gemini summary | Structured response + graceful failure |
| POST | /api/ai/cases/{caseId}/recommendation | Gemini recommendation | No consequential auto-action |
| POST | /api/audit/events | Create audit hash | No PII on-chain |
| GET | /api/audit/events/{id}/verify | Verify audit record | Re-hash current payload and compare |
| GET | /api/dashboard/kpis | Dashboard metrics | Calculated from DB / seed data |
| GET | /api/value-case | Savings/payback data | Expose assumptions separately from observed data |
| GET | /api/accounts/{accountId}/billing
| GET | /api/billing/exceptions/{id}

**Standard error response:**

```text
{
  "timestamp": "2026-09-26T14:00:00Z",
  "status": 400,
  "code": "INVALID_CASE_TRANSFER",
  "message": "Assigned team is required",
  "traceId": "..."
}
```

# 9. Functional Workflows

## 9.1 Complaint Resolution / Case Context

1.  Agent searches by Case ID / Account ID.

2.  Backend retrieves complaint, billing, meter, previous contacts, and field visit references from the canonical case + adapters.

3.  Assemble a Unified Case Context DTO; the frontend does not need to know which legacy system supplied each item.

4.  All actions append events to case_events and never overwrite history.

5.  Transfer only changes assignment and adds a TRANSFERRED event; keep the same Case ID.

6.  If a field visit is required, create the visit with the minimum necessary context; write FIELD_VISIT_COMPLETED back to the timeline when completed.

7.  After resolution / correction, send relevant data into the Billing / Meter feedback flow.

## 9.2 Billing Quality / Complaint Prevention

1.  Read meter / estimated-read data.

2.  The Deterministic Validation Engine calculates risk; do not depend on Gemini first.

3.  LOW risk -> normal flow; HIGH risk -> billing_exceptions queue.

4.  Analyst review: verify reading / request field visit / correct reading / approve.

5.  Correction stores original estimate, corrected value, reason, region, account, and date.

6.  Future validation uses correction history as a feature, forming a feedback loop.

**Initial risk rule (configurable; must not be hard-coded in the UI):**

```text
score = 0
if estimated_read:                       +25
if deviation_from_history > 50%:         +30
if consecutive_estimates >= 3:           +20
if previous_bill_corrections >= 1:       +15
if previous_billing_complaints >= 1:     +10

0-29   LOW
30-59  MEDIUM
60+    HIGH
```

# 10. External Technology Integration Specification

## 10.1 Gemini API

- Purpose: Complaint classification, Case summary, Recommended action, Customer-response draft.
- Use parseable Structured Output / JSON schema; the backend must validate output before sending it to the UI.
- Gemini does not perform anomaly math and does not directly execute refunds, bill changes, case closure, or compensation.
- If the API times out, hits quota, or returns invalid output, the core complaint workflow must remain usable; the UI shows AI temporarily unavailable.
- ai_analysis stores model, type, input hash, output JSON, and timestamp for explainability and traceability.
## 10.2 Tiger Data / PostgreSQL

- Use as the Resolve operational store; meter readings use a time-series-friendly schema.
- Persist case timeline, billing exceptions, correction feedback, AI output, and audit refs here.
- Migrations must be versioned; do not manually alter the production schema without a migration.
## 10.3 Solana Devnet

- Create hashes only for critical events such as BILL_CORRECTED, CASE_CLOSED, METER_OVERRIDE, and COMPENSATION_APPROVED.
- Hash payloads must use a canonical, stable serialization format; raw PII / complaint text / bill details must stay off-chain.
- Solana failure must not block the business action; set audit status = PENDING and retry later.
- Verify flow: regenerate payload hash -> read record / signature -> compare -> VERIFIED / MISMATCH / PENDING.
## 10.4 Vultr

- For the MVP, a single Cloud Compute / Docker deployment is sufficient; Kubernetes is not required.
- Run frontend, backend, and reverse proxy; Tiger Data may use an external hosted service.
- After deployment, CI must run /actuator/health or an equivalent health check.
# 11. Frontend Pages and Components

| **Page** | **Core Content** | **Required Actions** |
| --- | --- | --- |
| Executive Dashboard | Backlog, SLA, FCR, transfer, reopen, exceptions, estimated-read, savings | Filter / inspect trend / navigate to cases |
| Case Workspace | Case header, Gemini summary, timeline, billing, meter, field visit, audit | Transfer / request field visit / review recommendation / close flow |
| Billing Quality Monitor | Exception queue, risk score, reason codes, history | Review / verify / correct / request field visit |
| Audit View | Critical event, hash, Solana signature, verify status | Verify record |
| Value Case | Assumptions, gross savings, operating cost, net benefit, payback | Adjust allowed assumptions / view sensitivity |

**Frontend State Rule:** UI local state stores only view/filter/form state; Server State is determined by the API + database. Do not keep core case status only in Angular memory.

# 12. Backend Modules and Dependency Rules

| **Module** | **Responsibilities** | **Prohibited** |
| --- | --- | --- |
| cases | Case context, timeline, transfer, assignment, close use cases | Must not depend directly on the CaseTrack SDK/DB |
| billing | Exception, review, correction, approval use cases | Do not put risk rules in the controller/UI |
| metering | History, risk features, repeated estimate logic | Do not depend on Gemini for mathematical decisions |
| fieldforce | Visit request / status / result writeback | Do not expose unnecessary customer history |
| ai | Gemini orchestration / schema validation / fallback | Do not execute consequential actions |
| audit | Hash, Solana submit, retry, verify | Do not let chain outages block core workflows |
| analytics | KPI / value case calculations | Do not mix assumptions with observed facts |
| integrations | Adapters / mock providers | Domain services must not access raw external schemas directly |

```text
Controller -> Service / Use Case -> Repository or Provider Interface -> Adapter

Prohibited:
Angular -> Database
Controller -> raw SQL
CaseService -> CaseTrack SDK
Billing UI -> business risk calculation
```

# 13. Codex Implementation Sequence (Deliver by Phase)

| **Phase** | **Codex Task** | **Acceptance Criteria** |
| --- | --- | --- |
| Phase 0 - Bootstrap | Create repo, Angular, Spring Boot/Kotlin, Docker Compose, environment variable template, and README. | Frontend and backend start locally; health endpoint returns 200; no secrets committed. |
| Phase 1 - Contract & Schema | Create OpenAPI, error schema, DB migrations, and core entities/DTOs. | Swagger is viewable; DB can be rebuilt; API DTOs and schemas are tested. |
| Phase 2 - CSV Seed / Mock Legacy | Import six CSVs; create CaseTrack/MeterHub/FieldForce/Billing mock adapters. | Challenge rows are queryable after startup; backend does not read CSVs directly from the UI. |
| Phase 3 - Case Workspace | GET case context / timeline; Angular case search + detail. | A Case ID can be entered and complaint + timeline + meter/billing context is shown. |
| Phase 4 - Transfer & FieldForce | Transfer use case; field visit request/result; timeline append. | Transfer retains history; FieldForce result can be written back. |
| Phase 5 - Billing Quality | Risk engine, exception queue, review/correction, feedback. | HIGH risk can be reproduced; correction creates feedback; rules have unit tests. |
| Phase 6 - Gemini | Summary/classification/recommendation/draft; schema validation + fallback. | Case Workspace remains usable when the Gemini API is down; output is parseable when available. |
| Phase 7 - Solana Audit | Critical event hashing, Devnet submission, pending retry, verify. | A transaction/signature can be live-demonstrated; no PII is written on-chain. |
| Phase 8 - Dashboard & Value | KPI + savings/payback endpoints/UI. | Number sources are traceable; assumptions are separated from observed data. |
| Phase 9 - CI/CD & Vultr | GitHub Actions, Docker image, Vultr deploy, health check. | After the main branch passes, deploy automatically/semi-automatically; a public demo URL is available. |
| Phase 10 - E2E & Demo Hardening | Playwright, failure cases, demo seed, rehearsal. | Core demo path completes in one run; Gemini/Solana failures do not interrupt the main workflow. |

# 14. Testing Strategy

| **Type** | **Tool** | **Required Tests** |
| --- | --- | --- |
| Unit | JUnit | Risk rules; transfer keeps history; correction creates feedback; permissions/use-case rules |
| API / Controller | MockMvc | Status codes, validation, error schema, JSON contracts |
| DB Integration | Testcontainers PostgreSQL | Migrations, repositories, timeline persistence, exception state transitions |
| AI Contract | Mock Gemini + schema validation | Valid JSON; invalid output fallback; timeout fallback |
| Audit Integration | Mock + optional Devnet test | Hash deterministic; pending on failure; verify status |
| Frontend Component | Angular test | Rendering / form validation / loading / error states |
| E2E | Playwright | Case search -> summary -> field visit -> correction -> audit verify |

**Minimum required failure scenarios:**

- Gemini timeout -> show AI unavailable, while case / billing / meter data remains normal.
- Solana unavailable -> business event succeeds, audit = PENDING, and retry is available later.
- DB constraint / invalid transfer -> return the standard error response; do not create a partial event.
- FieldForce mock unavailable -> the case remains intact; show a retryable integration error.
- Duplicate request -> use an idempotency strategy or detect duplicates for critical mutations.
# 15. CI/CD and Deployment

```text
Git Push / Pull Request
        ↓
Frontend lint + tests
        ↓
Backend unit tests
        ↓
Integration tests
        ↓
Build Angular + Spring Boot
        ↓
Build Docker image
        ↓
(main only) Deploy to Vultr
        ↓
Health Check
```

- Branch: main, develop, feature/*; the hackathon workflow may be simplified, but main must remain demo-ready.
- Secrets: GEMINI_API_KEY, DATABASE_URL/PASSWORD, and SOLANA key/RPC must not be committed; provide .env.example.
- All DB schema changes must be version-controlled as migrations.
- A failed deployment must not automatically overwrite the last working version.
# 16. Security / Privacy / Reliability

- The Auth MVP may use simplified login, but the backend must still enforce authorization; do not rely only on hidden frontend buttons.
- Field engineers receive only the minimum necessary context for assigned cases.
- Do not log API keys, private keys, full PII, or sensitive complaint content.
- Gemini prompts should minimize transmitted data; the MVP uses synthetic data.
- Solana stores only hash / signature / non-sensitive metadata.
- The backend is stateless by default; core state is stored in the DB. Redis is not required for the MVP.
# 17. Observability and Maintenance

| **Item** | **Requirements** |
| --- | --- |
| Health | /actuator/health; DB, Gemini, and Solana may expose component status |
| Logs | Include at least traceId/requestId, caseId (non-sensitive identifier), endpoint, status, duration, and error code |
| Metrics | API latency, 5xx, AI failure rate, audit pending count, exception queue size |
| Documentation | Update README, API contract, schema, testing, and deployment documentation for every feature |
| ADR | Document the reason for any major architecture decision that deviates from this specification |

# 18. Value Case Engine

**Use challenge unit-cost inputs as the basis for calculations;**build/operating costs not provided by the data must be labeled as assumptions.

| **Input** | **Value / Rule** |
| --- | --- |
| Inbound call | $7.40 / call |
| Standard complaint | $68 / complaint |
| Transferred complaint | $121 / complaint |
| Manual bill correction | $34 / correction |
| Field meter visit | $92 / visit |
| Contact-centre FTE | $46,000 / FTE / year |
| Net benefit | Gross benefit - annual operating cost |
| Payback months | Build cost / (annual net benefit / 12) |

# 19. Definition of Done (Each Feature)

- API contract / DTO updated (if the feature affects the API).
- Domain logic is in the correct module; modules do not directly read each other's DB tables.
- Input validation, authorization, and error handling completed.
- Unit tests pass; features requiring DB / external integration have integration tests.
- Frontend has loading / empty / error states.
- External dependencies such as Gemini / Solana have graceful fallback.
- No secrets committed; lint / CI are green.
- README / relevant docs updated.
- Demo workflow runs end-to-end using synthetic data.
# 20. Execution Instructions for Codex

| **Codex must not implement all features at once.** Implement Phase 0 -> Phase 10 sequentially; before coding each phase, list the files, APIs, DB migrations, and tests to add/modify. After implementation, run tests and report results and incomplete items before moving to the next phase. |
| --- |

1.  Read README, this document, and docs/api-contract.yaml first; do not change the stack or architecture without instruction.

2.  Build a runnable skeleton first, then API contract / migrations, then features.

3.  For the MVP, all legacy integrations use adapters + synthetic CSV/mock data.

4.  Every mutation use case must consider validation, transaction boundaries, and idempotency / duplicate requests.

5.  All external APIs (Gemini, Solana) must be abstracted behind interfaces and mockable in tests.

6.  At the end of each phase, provide: changed files, how to run, tests run, known limitations, and next phase.

# 21. Final Demo Path

```text
1. Open Executive Dashboard
2. Search a high-risk billing complaint
3. Open Unified Case Workspace
4. Show full Case Timeline + Billing + Meter history
5. Generate Gemini Summary / Recommendation
6. Show abnormal estimated read / Billing Exception
7. Request / simulate FieldForce visit
8. Correct the reading / bill
9. Save correction feedback
10. Resolve case
11. Create / verify Solana audit hash
12. Return to dashboard and show KPI / value impact
```

**Demo Story Focus:**Angular presents it. Spring Boot controls it. Tiger Data remembers it. Gemini understands it. Solana verifies it. Vultr runs it.

# 22. Future / Post-Hackathon Backlog

- Replace mock adapters with real CaseTrack/MeterHub/FieldForce APIs where available.
- Targeted smart-meter rollout prioritization by repeated estimates/corrections/complaints.
- Improved estimation model using correction feedback data.
- Enterprise identity provider / SSO / RBAC.
- More robust event bus / asynchronous integration if scale requires it.
- Legacy retirement roadmap after integration data proves which systems drive cost/risk.
# Appendix A. Codex Kick-off Checklist

[ ] Repository created with frontend/backend/docs/data structure

[ ] Angular app runs

[ ] Spring Boot Kotlin app runs

[ ] /actuator/health returns 200

[ ] docker-compose local stack runs

[ ] OpenAPI contract created

[ ] DB migrations created

[ ] 6 synthetic CSV files placed under data/

[ ] Seed/import command documented

[ ] Case search + Case Workspace implemented

[ ] Timeline transfer test passes

[ ] Billing risk rules unit-tested

[ ] Gemini structured output + fallback implemented

[ ] Solana pending/verify flow implemented

[ ] Dashboard + value case implemented

[ ] GitHub Actions green

[ ] Vultr demo URL healthy

[ ] Playwright demo path passes
