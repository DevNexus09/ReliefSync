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

## Implemented foundation

Phase 1 provides the executable JavaFX foundation. Phase 2 adds:

- versioned and repeatable SQLite migrations
- the persistent base schema for all principal relief workflows
- typed domain models and entity-specific repositories
- PBKDF2 password hashing and persistent authentication
- centralized role-permission authorization
- optional, idempotent development seeding
- authenticated dashboard identity and logout

The advanced allocation, verification, lifecycle, dispatch, notification, and recovery behavior remains deferred to later phases.

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

Then sign in with username `admin` and password `ReliefSync@2026`. These are demo credentials only and must not be used for a real deployment. Later runs can use `mvn javafx:run`; the account remains in the local database.

The dashboard displays the authenticated user's name, username, role, and database status. Logout clears the in-memory session and returns to login.

The local database is created at `data/reliefsync.db`. Database files are runtime artifacts and are not committed.

## Architecture

```text
JavaFX Views → Controllers → Facade → Services → Repositories → SQLite
```

Phase 2 implements the database, domain, repository, authentication, authorization, and UI identity boundaries without introducing later workflow patterns. See [architecture.md](docs/architecture/architecture.md) for the dependency rules, [scope.md](docs/requirements/scope.md) for scope control, and [requirements](docs/requirements) for the requirements baseline.

## Development workflow

The two developers work from short-lived branches and merge through reviewed pull requests targeting `develop`:

1. Pull the latest `develop` branch.
2. Create a branch such as `feature/request-verification`.
3. Make focused commits with descriptive messages.
4. Run `mvn clean test` before pushing.
5. Open a pull request and have the other developer review it.
6. Merge into `develop` only after tests pass and review comments are resolved.

Do not commit IDE settings, generated build output, logs, or local SQLite database files. The complete collaboration policy and team-record placeholders are in [development-workflow.md](docs/development-workflow.md).
