# ReliefSync — Disaster Relief Resource Coordination System

ReliefSync is a JavaFX desktop application for coordinating scarce emergency resources during floods, cyclones, and similar disasters. It combines request verification, inventory control, stock allocation, transport dispatch, delivery recovery, reporting, and persistent in-application notifications in one compact Design Patterns Lab project.

The application is intentionally local and understandable: Java 21, JavaFX, Maven, JDBC, and SQLite are used without a web server or external framework.

## What problem it solves

Multiple affected areas may request food, water, medicine, and shelter materials while several relief centers hold limited stock. ReliefSync helps authorized coordinators answer:

- Is a request genuine, and which approval rounds are required?
- Which relief centers should supply the request?
- Is enough stock available, and what remains outstanding?
- Which available vehicle can carry the allocated load?
- Was the delivery successful, or does it need retry or reallocation?
- Which users should be notified about each operational event?

## Main workflow

```text
Draft → Submitted → Verified → Allocated → Dispatched → Delivered
            ↓ reject       ↓ cancel          ↓ failure
         Rejected       Cancelled       Delivery Failed
                                            ├─ Retry → Dispatched
                                            └─ Return stock → Allocated → Reallocate
```

An allocation may be partial when stock is insufficient. The request remains `ALLOCATED`, records its shortages, and can later receive outstanding stock through reallocation.

## Implemented features

### Authentication and authorization

- Login with persistent SQLite-backed user accounts.
- Self-service signup with full-name, username, password-strength, and confirmation validation.
- Passwords stored as salted PBKDF2-HMAC-SHA256 hashes; plain-text passwords are never stored.
- Public signup always creates the least-privileged `VOLUNTEER` role.
- Six roles with centralized feature grants; restricted sidebar pages are hidden and sensitive workflow operations are checked again in the service layer.
- In-memory authenticated session cleared on logout.

### Master data and inventory

- Search, create, update, activate, and deactivate affected areas, relief centers, and resources.
- Vehicle management with registration number, type, generic capacity, and availability status.
- Inventory quantities per relief-center/resource pair.
- Set, receive, and issue stock without permitting negative inventory.
- Configurable low-stock thresholds with `OK`, `LOW`, and `OUT OF STOCK` display states.
- Non-destructive deactivation preserves historical references.

### Relief requests and verification

- Multi-item request drafts with area, priority, notes, resources, and quantities.
- Warning when an affected area already has another open request.
- Submit and cancel actions with legal-state validation.
- Priority-dependent human verification rounds:
  - `NORMAL`: Area Coordinator
  - `HIGH`: Area Coordinator → Relief Coordinator
  - `CRITICAL`: Area Coordinator → Relief Coordinator → Administrator
- Administrator may perform any pending verification round.
- Any rejected round moves the request to `REJECTED`.
- Verification decisions and request status changes remain auditable.

### Allocation, release, and reallocation

- Read-only allocation preview before inventory changes.
- Two selectable strategies:
  - **Fewest Centers**: consumes larger stock positions first to reduce pickup points.
  - **Balanced Across Centers**: spreads withdrawals across available centers.
- Atomic initial allocation: inventory deduction, allocation rows, request-item totals, history, audit events, and notifications commit or roll back together.
- Partial allocation with explicit shortage reporting.
- Reallocation of outstanding quantities after new stock becomes available.
- Cancellation before dispatch by the request creator or Administrator.
- Allocated-request cancellation atomically releases every active reservation back to its source inventory.
- Immutable `ALLOCATED`, `REALLOCATED`, and `RELEASED` audit events.

### Vehicle assignment, dispatch, and delivery recovery

- Assignment of an available vehicle and driver to an allocated request.
- Capacity validation against the sum of active allocated quantities.
- Numbered dispatch attempts with vehicle, driver, assigned/dispatched time, allocation-backed load lines, and pickup-center details.
- Vehicle lifecycle: `AVAILABLE → IN_TRANSIT → AVAILABLE` after delivery or failure.
- Delivery confirmation moves the request and manifest to `DELIVERED`.
- Delivery failure records the reason, reporter, time, chosen recovery action, and optional recovery notes.
- `RETRY` uses an available capacity-safe vehicle and creates a new immutable dispatch attempt.
- `REALLOCATE` returns physically recovered stock, releases active allocations, resolves the failure, and returns the request to allocation processing.
- Maintenance and inactive vehicles cannot be dispatched.

### Notifications

- Persistent notifications for:
  - New requests awaiting verification
  - Completed verification rounds
  - Allocation and reallocation
  - True low-stock threshold crossings
  - Delivery failure
  - Successful delivery
- Role- and request-owner-based recipient selection.
- Every authenticated user sees only their own notifications.
- A top-right outlined bell displays a red unread-count badge; the badge is hidden at zero and capped visually at `99+`.
- Clicking the bell opens the full notification panel in the center of the application window.
- Users can mark one notification or all notifications as read, manually refresh, close by clicking outside, or press Escape.
- Deterministic event keys prevent duplicate notification rows.
- Low-stock warnings are suppressed while stock remains low and re-armed only after restocking above the threshold.

### Dashboard, reports, and interface

- Role-aware sidebar containing only permitted work areas.
- Dashboard summary cards.
- Low-stock report, requests-by-status summary, area-fulfillment analysis, and bounded request search.
- Request detail views include items, verification rounds, status history, allocations, allocation events, dispatch attempts, manifest items, and delivery failures.
- Professional JavaFX styling with consistent spacing, cards, tables, forms, active navigation, and status/priority badges.

## Role permissions

| Role | Main activities |
|---|---|
| Administrator | All features; all master data, inventory, requests, verification rounds, allocation, transport, vehicles, reports, and oversight notifications |
| Area Coordinator | Dashboard; create, submit, and cancel own requests; first verification round; reports |
| Relief Center Manager | Dashboard; affected areas, centers, resources, inventory, low-stock handling, and reports |
| Transport Coordinator | Dashboard; vehicles, dispatch, retry, delivery confirmation, failure reporting, and reports |
| Volunteer | Dashboard; create, submit, and cancel own requests |
| Relief Coordinator | Dashboard; second verification round, allocation/reallocation, failed-delivery return for reallocation, and reports |

All logged-in roles have the notification bell, but notification records are filtered to the current user. Request cancellation is additionally limited to the request creator or Administrator and must be legal for the request's current state.

## User interface structure

Authentication contains **Log in** and **Sign up** views. After login, the role-aware sidebar can contain:

1. Dashboard
2. Master Data (Affected Areas, Relief Centers, Resources, and/or Vehicles)
3. Inventory
4. Relief Requests
5. Allocation & Dispatch
6. Reports

Notifications are not a sidebar page. They are available globally from the bell between the role label and **Log out** in the top-right application bar.

## Design patterns

| Pattern | Implementation | Purpose |
|---|---|---|
| State | `com.reliefsync.state` | Encapsulates legal request lifecycle transitions |
| Chain of Responsibility | `com.reliefsync.verification` | Runs the priority-dependent approval sequence |
| Strategy | `com.reliefsync.strategy` | Selects interchangeable stock-allocation policies |
| Facade | `com.reliefsync.facade.ReliefOperationFacade` | Gives JavaFX one workflow-oriented API |
| Singleton | `com.reliefsync.db.Database` | Owns the single SQLite connection and transaction boundary |
| Repository | `com.reliefsync.repository` | Isolates SQL and row mapping from UI and business rules |
| Observer | `com.reliefsync.notification` | Converts domain events into recipient-specific persistent notifications |

Detailed pattern justifications are in [docs/patterns.md](docs/patterns.md). Layering, data ownership, role access, transaction boundaries, lifecycle diagrams, and the ER model are in [docs/architecture.md](docs/architecture.md).

## Technology and project structure

- Java 21
- JavaFX Controls 21.0.5
- Maven
- SQLite JDBC 3.46.1.3
- JUnit Jupiter 5.10.2

```text
src/main/java/com/reliefsync/
├── db/             Database lifecycle, transactions, migrations, demo seeding
├── facade/         Cross-service workflow API used by the UI
├── model/          Domain entities, enums, and read models
├── notification/   Observer subject, event, recipient policy, persistent observer
├── repository/     Parameterized SQL and row mapping
├── security/       PBKDF2 password hashing
├── service/        Authentication, authorization, and business workflows
├── state/          Request State implementations
├── strategy/       Allocation Strategy implementations
├── ui/             JavaFX scenes, panes, notification popup, and UI helpers
└── verification/   Verification Chain of Responsibility
```

## Prerequisites

- JDK 21 or newer
- Maven 3.9 or newer

Check the installed versions:

```bash
java -version
mvn -version
```

## Run and test

From the project root:

```bash
cd /path/to/ReliefSync
mvn clean test
mvn javafx:run
```

Run through Maven rather than launching `App.main()` directly from a plain IntelliJ configuration; the Maven JavaFX plugin supplies the required JavaFX module path.

The application creates or upgrades `data/reliefsync.db` automatically. JavaFX and SQLite may print native-access or `sun.misc.Unsafe` deprecation warnings on newer JDKs; with the currently configured dependencies these are runtime warnings and do not stop the application workflow.

## Demo data and accounts

Start once with the optional idempotent demonstration dataset:

```bash
RELIEFSYNC_SEED_DEMO=true mvn javafx:run
```

Windows PowerShell:

```powershell
$env:RELIEFSYNC_SEED_DEMO="true"; mvn javafx:run
```

All seeded users use password `ReliefSync@2026`:

| Username | Role |
|---|---|
| `admin` | Administrator |
| `area_coordinator` | Area Coordinator |
| `center_manager` | Relief Center Manager |
| `transport` | Transport Coordinator |
| `volunteer` | Volunteer |
| `relief_coordinator` | Relief Coordinator |

The seed includes 9 affected areas, 4 relief centers, 8 resources, 8 vehicles, 32 inventory lines, and 16 labelled request scenarios. Each request note begins with a stable key:

| Key | Demonstration | State |
|---|---|---|
| `DEMO-R01` | Untouched draft | DRAFT |
| `DEMO-R02` | Normal request awaiting verification | SUBMITTED |
| `DEMO-R03` | High request after round one | SUBMITTED |
| `DEMO-R04` | Critical request after rounds one and two | SUBMITTED |
| `DEMO-R05` | Verified request awaiting allocation | VERIFIED |
| `DEMO-R06` | Allocation strategy comparison | VERIFIED |
| `DEMO-R07` | Fully allocated request | ALLOCATED |
| `DEMO-R08` | Active dispatch | DISPATCHED |
| `DEMO-R09` | Successful delivery | DELIVERED |
| `DEMO-R10` | Rejected request | REJECTED |
| `DEMO-R11` | Cancelled allocation with released stock | CANCELLED |
| `DEMO-R12` | Unresolved delivery failure | DELIVERY_FAILED |
| `DEMO-R13` | Failed attempt retried with another vehicle | DISPATCHED |
| `DEMO-R14` | Partial allocation with medicine shortage | ALLOCATED |
| `DEMO-R15` | New stock followed by reallocation | ALLOCATED |
| `DEMO-R16` | Probable duplicate for an open area | SUBMITTED |

After the first seeded run, use normal `mvn javafx:run`; data and accounts remain in the local database.

### Suggested demonstration

1. Log in as `relief_coordinator`, open **Allocation & Dispatch**, select `DEMO-R06`, and preview both strategies. Previewing must not change inventory.
2. Inspect `DEMO-R14` and `DEMO-R15` to demonstrate shortage tracking and later reallocation.
3. Log in as `transport` to inspect `DEMO-R08`, `DEMO-R12`, and `DEMO-R13` for active dispatch, unresolved failure, and immutable retry attempts.
4. Open **Reports** to show status, fulfillment, and low-stock information.
5. Click the top-right bell under different accounts to demonstrate recipient-specific notifications and unread-state changes.

## Intentional scope limits

ReliefSync is a single-user desktop teaching application. It does not implement route optimization, GPS tracking, live multi-computer synchronization, email/SMS delivery, password recovery, privileged public signup, partial physical delivery, or resource-specific weight conversion. Vehicle capacity and allocated quantities use generic academic load units: one allocated unit consumes one vehicle-capacity unit.

## Development notes

- Schema migrations are applied automatically and recorded in `schema_version`.
- The application currently uses schema version 5 with 17 domain tables plus the migration metadata table.
- Demo seeding is optional and idempotent.
- Local database files, IDE settings, and Maven build output should not be committed.
- Run `mvn clean test` before merging changes.
