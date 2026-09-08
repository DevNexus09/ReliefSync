# ReliefSync — Disaster Relief Resource Coordination System

ReliefSync is a JavaFX desktop application that supports the coordination and distribution of scarce emergency resources during floods, cyclones, and similar disasters. It is a deliberately compact Design Patterns Lab project: small in surface area, but built around real decision-support workflows rather than plain CRUD forms.

## Core idea

During a disaster, several affected areas request food, water, medicine, and shelter materials while stock remains limited across multiple relief centers. ReliefSync answers:

- Which requests are genuine (multi-round verification by priority)
- Which relief centers should supply the requested resources (selectable allocation strategies)
- How limited stock is reserved, dispatched, and delivered (explicit lifecycle states)
- Where shortages and low stock remain (reports)

## Main workflow

```text
Draft → Submitted → Verified → Allocated → Dispatched → Delivered
            ↓ (reject)                (cancel possible until stock is reserved)
        Rejected                              Cancelled
```

## What is implemented

- SQLite persistence with versioned migrations and a meaningful schema (10 tables: users, affected areas, relief centers, resources, inventory, relief requests, request items, verifications, allocations, status history)
- PBKDF2 password hashing, persistent authentication, and centralized role-permission authorization for six roles
- CRUD with validation and non-destructive activate/deactivate for affected areas, relief centers, and resources
- Transactional inventory set/receive/issue with calculated low-stock status
- **Workflow 1 — request verification:** multi-item drafts, probable-duplicate warnings, submission, and a Chain of Responsibility that requires 1/2/3 human approval rounds for Normal/High/Critical priority, with immutable verification history
- **Workflow 2 — allocation to delivery:** strategy-based allocation planning with preview, transactional stock reservation with shortage reporting, dispatch, and delivery confirmation
- Reports and search: low-stock report, requests-by-status summary, fulfillment-by-area analysis, and bounded parameterized request search
- Optional, idempotent demo seeding that drives the real services to leave requests resting in five different lifecycle states; full status-history audit trail per request
- A styled interface (`src/main/resources/app.css`): dark sidebar with active-item highlighting, dashboard cards, and color-coded status/priority badges throughout
- 29 JUnit tests covering the patterns, validation, and an end-to-end workflow against a real SQLite database

## Screens (6)

Login · Dashboard · Master Data (areas / centers / resources) · Inventory · Relief Requests (draft + verification) · Allocation & Dispatch · Reports & Search

## Design patterns

| Pattern | Where | Problem it solves |
|---|---|---|
| State | `com.reliefsync.state` | Legal lifecycle transitions per request status without if/else chains |
| Chain of Responsibility | `com.reliefsync.verification` | Priority-dependent number of human verification rounds |
| Strategy | `com.reliefsync.strategy` | Interchangeable stock-distribution policies with preview |
| Facade | `com.reliefsync.facade.ReliefOperationFacade` | One workflow API for the UI across services |
| Singleton | `com.reliefsync.db.Database` | Single owned SQLite connection and transaction scope |
| Repository | `com.reliefsync.repository` | SQL isolated from business logic and UI |

Full justifications (problem, alternatives, future benefits) are in [docs/patterns.md](docs/patterns.md); layering rules and the ER diagram are in [docs/architecture.md](docs/architecture.md).

## Prerequisites

- JDK 21+ (build targets `--release 21`)
- Maven 3.9+

## Run and test

```bash
mvn clean test
mvn javafx:run
```

Normal startup migrates the database but does not create a known user. For a local demonstration, start once with seeding enabled:

```bash
RELIEFSYNC_SEED_DEMO=true mvn javafx:run
```

On Windows PowerShell:

```powershell
$env:RELIEFSYNC_SEED_DEMO="true"; mvn javafx:run
```

All local demo accounts use password `ReliefSync@2026`:

| Username | Role |
|---|---|
| `admin` | Administrator |
| `area_coordinator` | Area Coordinator |
| `center_manager` | Relief Center Manager |
| `transport` | Transport Coordinator |
| `volunteer` | Volunteer |
| `relief_coordinator` | Relief Coordinator |

These are demo credentials only. Later runs can use plain `mvn javafx:run`; accounts persist in the local database at `data/reliefsync.db` (a runtime artifact, not committed).

Seeding also creates five demo requests resting in different states — one DELIVERED (three approval rounds), one VERIFIED and awaiting allocation, one SUBMITTED mid-chain, one REJECTED, and one DRAFT — so the dashboard and reports have content on first launch.

### Demonstration walkthrough

1. Log in as `relief_coordinator` and open **Allocation & Dispatch**. Select the VERIFIED request, press **Preview plan**, then switch the strategy dropdown and preview again — the same request produces a completely different distribution (one center vs. split across three). This is the Strategy pattern demonstrated live, with no data written.
2. Open **Relief Requests** and press **Details** on the DELIVERED request to see its three verification rounds and its full status-history audit trail.
3. Log in as `volunteer`, create a new HIGH-priority draft with two items, and submit it. Picking an area that already has an open request triggers the duplicate warning.
4. Log in as `area_coordinator` and approve — the request stays SUBMITTED because a HIGH request needs a second round (Chain of Responsibility). Log in as `relief_coordinator` to approve round 2; it becomes VERIFIED.
5. Allocate it, then log in as `transport` to dispatch and confirm delivery. Check **Reports** for the stock impact and fulfillment percentages.

## Development workflow

Both developers work from short-lived feature branches merged into `main` through reviewed pull requests. Run `mvn clean test` before pushing. Do not commit IDE settings, build output, or local SQLite database files.
