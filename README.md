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
            ↓ (reject)          ↓ (cancel)      ↓ (failure)
        Rejected             Cancelled     Delivery Failed
                                             ↙       ↘
                                          Retry   Reallocate
```

## What is implemented

- SQLite persistence with versioned migrations and a meaningful 17-table schema (version 5), including reversible allocations, allocation audit events, vehicles, dispatch attempts, delivery failures, notifications, and low-stock alert state
- PBKDF2 password hashing, persistent login and self-service signup, and centralized role-permission authorization for six roles; public signup safely creates Volunteer accounts only
- CRUD with validation and non-destructive activate/deactivate for affected areas, relief centers, resources, and transport vehicles
- Transactional inventory set/receive/issue with calculated low-stock status
- **Workflow 1 — request verification:** multi-item drafts, probable-duplicate warnings, submission, and a Chain of Responsibility that requires 1/2/3 human approval rounds for Normal/High/Critical priority, with immutable verification history
- **Workflow 2 — allocation to delivery and recovery:** strategy-based allocation planning with preview, transactional stock reservation, later reallocation of outstanding need, cancellation with atomic stock release, capacity-validated dispatch attempts, delivery confirmation, and a `DELIVERY_FAILED` path that supports vehicle-backed retry or explicit stock return for reallocation
- **In-application notifications:** persistent role/ownership-targeted updates for requests awaiting verification, completed verification rounds, allocation/reallocation, true low-stock threshold crossings, delivery failures, and successful deliveries; users can view unread counts and mark one or all notifications read
- Reports and search: low-stock report, requests-by-status summary, fulfillment-by-area analysis, and bounded parameterized request search
- Optional, idempotent Bangladesh-context demo seeding with 9 affected areas, 4 relief centers, 8 resources, 8 vehicles, and 16 labelled workflow scenarios covering every request state, strategy comparison, shortage, cancellation, retry, reallocation, low stock, and notifications
- A styled interface (`src/main/resources/app.css`): dark sidebar with active-item highlighting, dashboard cards, and color-coded status/priority badges throughout
- 76 JUnit tests covering authentication/signup, all patterns, validation, backward-compatible migrations through schema v5, notification recipients/idempotency/read state/rollback, realistic seed-data invariants, low-stock re-arming, reservation release, reallocation, vehicles, dispatch attempts, delivery recovery, and end-to-end workflows against a real SQLite database

## Screens (8)

Login and Signup · Dashboard · Master Data (areas / centers / resources / vehicles) · Inventory · Relief Requests (draft + verification) · Allocation & Dispatch · Reports & Search · Notifications

## Design patterns

| Pattern | Where | Problem it solves |
|---|---|---|
| State | `com.reliefsync.state` | Legal lifecycle transitions per request status without if/else chains |
| Chain of Responsibility | `com.reliefsync.verification` | Priority-dependent number of human verification rounds |
| Strategy | `com.reliefsync.strategy` | Interchangeable stock-distribution policies with preview |
| Facade | `com.reliefsync.facade.ReliefOperationFacade` | One workflow API for the UI across services |
| Singleton | `com.reliefsync.db.Database` | Single owned SQLite connection and transaction scope |
| Repository | `com.reliefsync.repository` | SQL isolated from business logic and UI |
| Observer | `com.reliefsync.notification` | Synchronous domain events become persistent, recipient-specific notifications without coupling workflows to the UI |

Full justifications (problem, alternatives, future benefits) are in [docs/patterns.md](docs/patterns.md); layering rules and the ER diagram are in [docs/architecture.md](docs/architecture.md).

## Prerequisites

- JDK 21+ (build targets `--release 21`)
- Maven 3.9+

## Run and test

```bash
mvn clean test
mvn javafx:run
```

Normal startup migrates the database and allows a new user to register as a Volunteer from the **Sign up** option. For role-specific local demonstrations, start once with seeding enabled:

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

The realistic seed contains 32 center/resource inventory lines and 16 requests with 35 request items. Each request note starts with a stable demonstration key:

| Key | Demonstration | Final state |
|---|---|---|
| `DEMO-R01` | Untouched request draft | DRAFT |
| `DEMO-R02` | Normal request awaiting its verifier | SUBMITTED |
| `DEMO-R03` | High request after round one | SUBMITTED |
| `DEMO-R04` | Critical request after rounds one and two | SUBMITTED |
| `DEMO-R05` | Verified request awaiting allocation | VERIFIED |
| `DEMO-R06` | Allocation strategy comparison | VERIFIED |
| `DEMO-R07` | Fully allocated request | ALLOCATED |
| `DEMO-R08` | Active dispatch | DISPATCHED |
| `DEMO-R09` | Successful delivered request | DELIVERED |
| `DEMO-R10` | Rejected assessment | REJECTED |
| `DEMO-R11` | Allocation cancelled and stock released | CANCELLED |
| `DEMO-R12` | Unresolved delivery failure | DELIVERY_FAILED |
| `DEMO-R13` | Failed attempt retried with another vehicle | DISPATCHED |
| `DEMO-R14` | Partial allocation with medicine shortage | ALLOCATED |
| `DEMO-R15` | New stock followed by reallocation | ALLOCATED |
| `DEMO-R16` | Probable duplicate for an open area | SUBMITTED |

### Demonstration walkthrough

1. Log in as `relief_coordinator`, open **Allocation & Dispatch**, select `DEMO-R06`, and preview both strategies. **Fewest Centers** uses the largest stockpiles first, while **Balanced Across Centers** visibly splits the same request across more centers without writing data.
2. Open **Relief Requests** and inspect `DEMO-R04` to see two completed Critical verification rounds with Administrator approval still pending. Inspect `DEMO-R09` for the completed three-round chain and delivery history.
3. Inspect `DEMO-R11` for inactive allocations and release events, `DEMO-R14` for outstanding medicine need, and `DEMO-R15` for separate allocation and reallocation audit events.
4. Log in as `transport`. `DEMO-R08` has one active manifest, `DEMO-R12` has an unresolved delivery failure awaiting reallocation recovery, and `DEMO-R13` preserves its failed first attempt plus active retry attempt.
5. Open **Reports** for the varied status summary, area fulfillment, and low-stock lines. Open **Notifications** under different role accounts to see recipient-specific submission, verification, allocation, low-stock, failure, and delivery events.

Vehicle capacity is intentionally modeled as generic load units for this compact academic application: each allocated resource quantity consumes one capacity unit, even though real resources use different physical units.

## Development workflow

Both developers work from short-lived feature branches merged into `main` through reviewed pull requests. Run `mvn clean test` before pushing. Do not commit IDE settings, build output, or local SQLite database files.
