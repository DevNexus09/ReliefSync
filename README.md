# ReliefSync — Disaster Relief Resource Coordination System

ReliefSync is a JavaFX desktop application designed to support the coordination and distribution of scarce emergency resources during floods, cyclones, severe waterlogging, and similar disasters.

The system coordinates affected areas, relief centers, volunteers, coordinators, supplies, allocations, transport, and deliveries through structured decision-support workflows rather than functioning as a simple CRUD application.

## Core idea

During a disaster, several affected areas may request food, water, medicine, shelter materials, and other emergency supplies while stock remains limited across multiple relief centers.

ReliefSync helps determine:

- Which affected areas should receive resources first
- Which relief centers should supply the requested resources
- How limited stock should be distributed
- How requests should be verified
- How reserved resources should be dispatched and delivered
- How shortages, failures, and remaining needs should be handled

The central problem is:

> How should limited relief resources be distributed among affected areas according to urgency, need, availability, distance, waiting time, transport capacity, and delivery risk?

## Main workflow

```text
Affected Area
     ↓
Relief Request
     ↓
Verification
     ↓
Resource Allocation
     ↓
Inventory Reservation
     ↓
Dispatch
     ↓
Delivery
     ↓
Confirmation
```

## Implemented phases

Phase 1 provides the executable JavaFX foundation. Phase 2 adds:

- versioned and repeatable SQLite migrations
- the persistent base schema for all principal relief workflows
- typed domain models and entity-specific repositories
- PBKDF2 password hashing and persistent authentication
- centralized role-permission authorization
- optional, idempotent development seeding
- authenticated dashboard identity and logout

Phase 3 adds role-aware core management for:

- disaster events and explicit close/reactivate lifecycle actions
- affected-area assessment data
- relief centers and resources with non-destructive activation controls
- transactional inventory initialization and adjustment with audit history
- calculated available/low-stock status without storing derived availability
- vehicle registration, home-center assignment, and safe manual availability
- bounded, parameterized repository search across all six modules

Phase 4 adds the first complete decision-support workflow:

- in-memory, validated multi-item relief-request drafts
- probable-duplicate warnings with explicit merge, continue, or cancel decisions
- a genuine State implementation for all ten request lifecycle states
- dynamic Normal, High, and Critical verification policies
- a Chain of Responsibility with one human decision per login/action
- immutable verification history separated by verification round
- transactional request submission and verification decisions
- role-specific request and verification workspaces

Allocation, reservation, dispatch, delivery, notification, recovery, reallocation, and analytics remain deferred to later phases.

## Prerequisites

- JDK 21
- Maven 3.9+
- Git

Check the development environment:

```bash
java -version
mvn -version
git --version
```

The build targets Java 21. A newer JDK can be used when it supports compiling with `--release 21`.

## Run and test

```bash
mvn clean test
mvn javafx:run
```

Normal startup migrates the database but does not create a known user. For a local university demonstration, start once with:

```bash
RELIEFSYNC_SEED_DEMO=true mvn javafx:run
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

These are demo credentials only and must not be used for a real deployment. Later runs can use `mvn javafx:run`; the accounts remain in the local database.

The dashboard displays the authenticated user's name, username, role, and database status. Logout clears the in-memory session and returns to login.

The local database is created at `data/reliefsync.db`. Database files are runtime artifacts and are not committed.

## Architecture

```text
JavaFX Views → Controllers → Facade → Services → Repositories → SQLite
```

Phase 4 implements State for lifecycle-dependent request behavior and Chain of Responsibility for variable-depth human verification. The planned `ReliefOperationFacade` remains reserved for later cross-service operational workflows. See [architecture.md](docs/architecture/architecture.md) for the dependency rules, [scope.md](docs/requirements/scope.md) for scope control, and [requirements](docs/requirements) for the requirements baseline.

## Development workflow

The two developers work from short-lived branches and merge through reviewed pull requests targeting `main`:

1. Pull the latest `main` branch.
2. Create a branch such as `feature/request-verification`.
3. Make focused commits with descriptive messages.
4. Run `mvn clean test` before pushing.
5. Open a pull request and have the other developer review it.
6. Merge into `main` only after tests pass and review comments are resolved.

Do not commit IDE settings, generated build output, logs, or local SQLite database files. The complete collaboration policy and team-record placeholders are in [development-workflow.md](docs/development-workflow.md).
