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

## Phase 1 foundation

Phase 1 provides the executable foundation only:

- Maven configuration
- JavaFX login and dashboard views
- Centralized navigation and shared CSS
- SQLite connectivity checks
- Architecture, scope, requirements, and collaboration documentation
- Automated application, FXML, navigation, and database smoke tests

Operational database tables and business features are deferred to their planned implementation phases.

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

The application starts on a placeholder login screen. Select **Continue** to verify FXML navigation. On startup, ReliefSync runs SQLite connection checks and logs the detected SQLite version. The dashboard also displays whether its database connection is available.

The local database is created at `data/reliefsync.db`. Database files are runtime artifacts and are not committed.

## Architecture

```text
JavaFX Views → Controllers → Facade → Services → Repositories → SQLite
```

Phase 1 implements only the JavaFX application shell and database connection boundary. See [architecture.md](docs/architecture/architecture.md) for the dependency rules, [scope.md](docs/requirements/scope.md) for scope control, and [requirements](docs/requirements) for the requirements baseline.

## Development workflow

The two developers work from short-lived branches and merge through reviewed pull requests targeting `develop`:

1. Pull the latest `develop` branch.
2. Create a branch such as `feature/request-verification`.
3. Make focused commits with descriptive messages.
4. Run `mvn clean test` before pushing.
5. Open a pull request and have the other developer review it.
6. Merge into `develop` only after tests pass and review comments are resolved.

Do not commit IDE settings, generated build output, logs, or local SQLite database files. The complete collaboration policy and team-record placeholders are in [development-workflow.md](docs/development-workflow.md).
