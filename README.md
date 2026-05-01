# myRepository

A personal full-stack monorepo built on **Hexagonal Architecture**, managing Finance and Health domains with a React frontend.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Backend Language | Java 25 |
| Backend Framework | Quarkus 3.29.0 |
| Build Tool | Gradle 9.3.0 (Kotlin DSL) |
| Database | PostgreSQL (single database `app_db`) |
| Schema Migrations | Flyway |
| API Contract | OpenAPI 3.1.0 (contract-first, Google AIP) |
| Frontend | React (JavaScript) |

---

## Repository Structure

```
myRepository/
├── application/                  # All backend domain services
│   ├── finance/                  # Finance domain (Quarkus service)
│   │   └── src/main/
│   │       ├── java/com/myrepository/finance/
│   │       │   ├── domain/           # Pure business logic
│   │       │   ├── ports/
│   │       │   │   ├── in/           # Driving interfaces (use cases)
│   │       │   │   └── out/          # Driven interfaces (repositories)
│   │       │   ├── application/      # Use case implementations
│   │       │   ├── adapters/
│   │       │   │   ├── in/http/      # REST controllers
│   │       │   │   └── out/persistence/ # DB adapters
│   │       │   └── mappers/          # DTO ↔ domain model transformations
│   │       └── resources/
│   │           ├── application.properties
│   │           └── db/migration/     # Flyway DDL scripts
│   │               ├── V1__create_account_table.sql
│   │               ├── V2__create_transaction_table.sql
│   │               └── V3__create_goal_table.sql
│   │
│   └── health/                   # Health domain (Quarkus service)
│       └── src/main/
│           ├── java/com/myrepository/health/
│           │   ├── domain/
│           │   ├── ports/in/
│           │   ├── ports/out/
│           │   ├── application/
│           │   ├── adapters/in/http/
│           │   ├── adapters/out/persistence/
│           │   └── mappers/
│           └── resources/
│               ├── application.properties
│               └── db/migration/
│                   ├── V4__create_health_profile_table.sql
│                   ├── V5__create_doctor_visit_table.sql
│                   └── V6__create_vital_reading_table.sql
│
├── openapi/                      # OpenAPI contracts (contract-first)
│   ├── finance.yaml              # Finance API contract
│   └── health.yaml               # Health API contract
│
├── infrastructure/               # Shared plumbing (DB connections, messaging)
├── shared/                       # Cross-cutting concerns (auth, logging, errors)
│
├── ui/                           # Frontend monorepo
│   └── web/                      # React (JavaScript) app
│       └── src/
│           └── api/generated/    # Auto-generated API clients from OpenAPI contracts
│
├── build.gradle.kts              # Root Gradle build
├── settings.gradle.kts           # Module registration
├── gradle.properties             # JVM and Gradle configuration
└── gradle/wrapper/
    └── gradle-wrapper.properties # Gradle 9.3.0
```

---

## Architecture — Hexagonal (Ports and Adapters)

Each domain inside `application/` is a self-contained hexagon. The core rule is that the **domain never depends on anything outside itself**.

```
                    ┌─────────────────────────────────┐
                    │           DOMAIN                 │
   HTTP Request ──► │  ports/in  →  application  →    │ ──► Database
   Message     ──► │  (use case interfaces)           │ ──► External API
                    │            ↕                     │
                    │         domain/                  │
                    │  (entities, services, logic)     │
                    │            ↕                     │
                    │  ports/out                       │
                    │  (repository interfaces)         │
                    └─────────────────────────────────┘
                           ↑                  ↑
                    adapters/in/        adapters/out/
                    (controllers)       (DB, HTTP clients)
```

### Key Rules

1. `domain/` — zero imports from adapters, infrastructure, or any framework. Plain Java only.
2. `ports/in/` — interfaces defining what use cases are available. Implemented by `application/` classes.
3. `ports/out/` — interfaces defining what the domain needs from outside. Implemented by `adapters/out/`.
4. `application/` — use cases only. Orchestrates domain logic. No direct DB or HTTP calls.
5. `adapters/out/` — imports from `infrastructure/` for shared clients. Never imports from another domain.
6. `infrastructure/` — shared wiring only (DB connection pool, messaging clients, config). No business logic.
7. `shared/` — cross-cutting utilities (auth, logging, error types). No dependency on any domain.
8. **Domains never import each other.** Cross-domain communication uses events via `infrastructure/messaging/`.

---

## Data Model

### Single Database (`app_db`)

All domains share one PostgreSQL database with clearly separated tables. Each domain owns its own tables — no cross-domain joins.

### Finance Domain

| Table | Purpose |
|---|---|
| `account` | All accounts — savings, current, credit card, loans, investments, NPS, PPF, FD |
| `transaction` | Every money movement — income, expense, transfer, investment buy/sell, loan EMI |
| `goal` | Financial goals with target amount, monthly saving, and days-to-completion calculation |

**Key design decision — no double counting:**

Credit card bill payments and loan EMIs are stored as `TRANSFER` type transactions. Real expense queries filter to `DEBIT` and `EXPENSE` types only, so internal money movements are never counted as expenses.

```
Example: ₹20,000 credit card bill payment from savings

  Row 1 → account: HDFC Savings,      type: TRANSFER, amount: 20,000
  Row 2 → account: HDFC Credit Card,  type: TRANSFER, amount: 20,000

  Real expense query: WHERE txn_type = 'EXPENSE'  → ₹0 (correctly excluded)
  Net worth query:    SUM all account balances     → transfers cancel out
```

**Transaction types:**

| Type | Meaning |
|---|---|
| `INCOME` | Salary, rental, freelance |
| `INTEREST_EARNED` | Savings or FD interest |
| `EXPENSE` | UPI payment, credit card swipe |
| `LOAN_INTEREST` | Interest portion of EMI (real cost) |
| `BANK_CHARGE` | Fees, penalties |
| `TRANSFER` | Internal movement (CC bill, loan EMI principal) |
| `INVESTMENT_BUY` | SIP or lumpsum purchase |
| `INVESTMENT_SELL` | Redemption |

### Health Domain

| Table | Purpose |
|---|---|
| `health_profile` | One row per person — self and family members |
| `doctor_visit` | Doctor visits and illness periods (no-visit flag for sick days without a doctor) |
| `vital_reading` | BP, weight, blood sugar, heart rate — single readings over time |

---

## OpenAPI Contract (Contract-First)

Contracts are written first in `openapi/` before any backend code. Backend controllers must match the contract. Frontend uses auto-generated clients from the same contract.

```
openapi/finance.yaml  →  Backend: AccountResource.java must implement it
                      →  Frontend: npm run generate:api creates typed client
```

Both contracts follow:
- **OpenAPI 3.1.0** — latest specification
- **Google AIP** — resource-oriented design, standard methods, consistent naming

### API conventions

| Convention | Rule |
|---|---|
| Resource URLs | Plural nouns — `/v1/accounts`, `/v1/health-profiles` |
| Create | `POST /v1/accounts` |
| Get one | `GET /v1/accounts/{account_id}` |
| List | `GET /v1/accounts` with `page_size` and `page_token` |
| Update | `PATCH /v1/accounts/{account_id}` (partial update only) |
| Delete | `DELETE /v1/accounts/{account_id}` (soft delete) |
| Field names | `snake_case` throughout |
| Error format | `{ code, status, message, details }` |

---

## Database Migrations (Flyway)

Schema is managed as code using Flyway. Migrations run automatically on Quarkus startup.

```
db/migration/
├── V1__create_account_table.sql        ← Finance
├── V2__create_transaction_table.sql    ← Finance
├── V3__create_goal_table.sql           ← Finance
├── V4__create_health_profile_table.sql ← Health
├── V5__create_doctor_visit_table.sql   ← Health
└── V6__create_vital_reading_table.sql  ← Health
```

**Flyway rules — never break these:**
- Never edit a migration file that has already run
- Always add a new file for schema changes
- Version numbers must be sequential
- Description uses double underscore: `V1__description.sql`

---

## Local Development Setup

### Prerequisites

| Tool | Version |
|---|---|
| Java | 25.0.x |
| Gradle | 9.3.0 (via wrapper — no install needed) |
| PostgreSQL | Any recent version, running locally |
| Node.js | 18+ |
| IntelliJ IDEA | Any recent version |

### Step 1 — Clone and open

```bash
git clone <repo-url>
cd myRepository
```

Open in IntelliJ → **File → Open** → select the root folder → Gradle auto-syncs.

### Step 2 — Create PostgreSQL database and user

In pgAdmin or psql:

```sql
CREATE DATABASE app_db;
CREATE USER app_user WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE app_db TO app_user;
```

### Step 3 — Create `.env` files

Create `application/finance/.env`:

```properties
DB_USER=app_user
DB_PASSWORD=yourpassword
DB_URL=jdbc:postgresql://localhost:5432/app_db
```

Create `application/health/.env` with the same content.

> `.env` files are gitignored and never committed.

### Step 4 — Run Finance service

```bash
./gradlew :application:finance:quarkusDev
```

Flyway runs automatically on first start and creates all tables in `app_db`.

### Step 5 — Run Health service

```bash
./gradlew :application:health:quarkusDev
```

### Step 6 — Run React frontend

```bash
cd ui/web
npm install
npm start
```

App runs at `http://localhost:3000`.

### Step 7 — View API docs

Once a service is running, open:

```
http://localhost:8080/swagger-ui
```

---

## Generating the React API client

The frontend API client is auto-generated from the OpenAPI contracts. Re-run whenever a contract changes:

```bash
cd ui/web
npm run generate:api
```

Generated client is written to `ui/web/src/api/generated/`.

---

## Known Setup Notes

- **JVM args:** Java 25 requires `--add-opens java.base/java.lang=ALL-UNNAMED` for Quarkus internals. This is configured in `build.gradle.kts` under `quarkusDev { jvmArgs }`.
- **Single database:** Both domains point to `app_db`. Flyway migration versions are globally sequential (V1–V3 Finance, V4–V6 Health) to avoid conflicts.
- **No Docker required:** PostgreSQL runs locally. No containerisation needed for local development.

---

## What is coming next

- [ ] `AccountResource.java` — first REST controller matching `openapi/finance.yaml`
- [ ] `HealthProfileResource.java` — first REST controller matching `openapi/health.yaml`
- [ ] Transaction endpoints — create, list, filter by type
- [ ] Goal tracker — days-to-target calculation endpoint
- [ ] React UI — account dashboard, transaction list
- [ ] NAV fetch — AMFI API integration for mutual fund current value