# ReliefSync

ReliefSync is a JavaFX desktop application for coordinating scarce disaster-relief resources. The project is intentionally designed as a decision-support and workflow application rather than a collection of CRUD screens.

Phase 1 provides the executable foundation only: Maven configuration, JavaFX navigation, shared styling, SQLite connectivity, architectural documentation, and smoke tests. Operational tables and business features are deferred to later phases.

## Prerequisites

- JDK 21
- Maven 3.9+
- Git

Check the tools installed on a development machine:

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

The application starts on a placeholder login screen. Select **Continue to dashboard** to verify FXML navigation. On startup, ReliefSync also runs a SQLite query and logs the detected SQLite version.

The local database is created at `data/reliefsync.db`. Database files are runtime artifacts and are not committed.

## Architecture

```text
JavaFX Views -> Controllers -> Facade -> Services -> Repositories -> SQLite
```

Phase 1 implements only the JavaFX application shell and database connection boundary. See [architecture.md](docs/architecture.md) for the full dependency rules, [scope.md](docs/scope.md) for scope control, and [requirements](docs/requirements) for the requirements baseline.

## Development workflow

The two developers work from short-lived branches and merge through reviewed pull requests:

1. Pull the latest `main` branch.
2. Create a branch such as `feature/phase-1-foundation` or `feature/request-verification`.
3. Make focused commits with descriptive messages.
4. Run `mvn clean test` before pushing.
5. Open a pull request and have the other developer review it.
6. Merge only after tests pass and review comments are resolved.

Do not commit IDE settings, generated build output, logs, or local SQLite database files.

The complete collaboration policy and team-record placeholders are in [development-workflow.md](docs/development-workflow.md).
