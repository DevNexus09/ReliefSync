# ReliefSync Architecture

## Purpose

ReliefSync is organized around relief coordination and decision-support workflows. CRUD operations support those workflows but do not define the application architecture.

## Target dependency flow

```text
JavaFX Views (FXML + CSS)
        ↓
Controllers
        ↓
ReliefOperationFacade
        ↓
Services and pattern components
        ↓
Repositories / DAOs
        ↓
SQLite
```

Phase 1 implements only this executable subset:

```text
ReliefSyncApplication → SceneManager → FXML views
ReliefSyncApplication → DatabaseHealthCheck → DatabaseManager → SQLite
Controllers → NavigationService → SceneManager
```

No business feature depends on persistence during Phase 1.

## Runtime boundary

ReliefSync is a local Java 21 desktop application. Phase 1 uses Maven, JavaFX, JDBC, and an embedded SQLite database only. It does not require Node.js, Python, Docker, a separate backend service, or an external database server.

## Layer responsibilities

### Presentation

FXML and CSS describe the interface. Controllers gather input, display results, and invoke high-level application operations. Controllers must not execute SQL or calculate allocations.

### Facade

`ReliefOperationFacade` will expose complete business use cases and coordinate services. Its package is reserved in Phase 1; the facade itself will be introduced when a real workflow requires it.

### Services and patterns

Services will own validation, authorization, transactions, and business workflows. Strategy, State, Chain of Responsibility, Observer, Command, Factory Method, and Facade components will be introduced only with the corresponding business problem.

### Repositories

Repositories will execute persistence operations and map database rows to models. SQLite-specific implementations will live in `repository.sqlite`. Repositories must not depend on JavaFX, controllers, or allocation strategies.

### Database

SQLite will persist operational and audit data. Phase 1 creates only an application-local database file and verifies connectivity; it creates no operational tables.

## Dependency rules

Allowed direction:

```text
Controller → Facade → Service → Repository → Database
```

Pattern components may be used by services or the facade.

Forbidden dependencies:

- Repository → Controller
- Model → JavaFX Controller
- FXML → SQL or business logic
- Controller → JDBC
- Strategy → JavaFX
- Repository → Strategy

## Navigation

`View` defines each FXML path and window title in one place. `SceneManager` owns the primary stage, loads FXML, applies the shared stylesheet, replaces scenes, and reports navigation failures. Controllers never create additional stages. `NavigationService` gives controllers semantic navigation operations without exposing FXML paths.

## Database connection

`DatabaseConfig` resolves the application-local database path. `DatabaseManager` creates its parent directory, opens JDBC connections, and enables SQLite foreign keys per connection. `DatabaseHealthCheck` owns the Phase 1 `SELECT 1` query. Callers use try-with-resources so connections, statements, and result sets close deterministically.

## Decisions deferred beyond Phase 1

- Operational schema, migrations, and seed data
- Domain models and repositories
- Authentication and authorization
- Allocation contracts and formulas
- Request, allocation, and dispatch lifecycles
- The seven required GoF pattern implementations
