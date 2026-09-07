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

Phases 2 and 3 implement this executable subset:

```text
ReliefSyncApplication → SchemaInitializer → MigrationRunner → SQLite
login-view.fxml → LoginController → AuthenticationService → UserRepository → SQLite
AuthenticationService → PasswordHasher + SessionManager
DashboardController → SessionManager
Core Management Controllers → Services → Repositories → SQLite
Services → Validators + AuthorizationService
InventoryService → InventoryTransactionRepository → TransactionManager → SQLite
Controllers → NavigationService → SceneManager → FXML views
```

`ApplicationContext` performs manual dependency wiring, and `ControllerFactory` supplies constructor-injected controllers through `FXMLLoader`. Neither is presented as one of the required GoF patterns.

## Runtime boundary

ReliefSync is a local Java 21 desktop application. It uses Maven, JavaFX, JDBC, and an embedded SQLite database only. It does not require Node.js, Python, Docker, a separate backend service, or an external database server.

## Layer responsibilities

### Presentation

FXML and CSS describe the interface. Controllers gather input, display results, and invoke high-level application operations. Controllers must not execute SQL or calculate allocations.

### Facade

`ReliefOperationFacade` will expose complete business use cases and coordinate services. Its package remains reserved; the facade itself will be introduced when a later workflow requires it.

### Services and patterns

Services own validation, authorization, transactions, and business workflows. During Phase 3, core management controllers call one service directly; the facade remains reserved for later multi-service workflows. Strategy, State, Chain of Responsibility, Observer, Command, Factory Method, and Facade components will be introduced only with the corresponding business problem.

### Repositories

Repositories will execute persistence operations and map database rows to models. SQLite-specific implementations will live in `repository.sqlite`. Repositories must not depend on JavaFX, controllers, or allocation strategies.

### Database

SQLite persists the base schema and Phase 3 search indexes. `DatabaseManager` configures every connection, `MigrationRunner` applies versioned classpath migrations transactionally, and `TransactionManager` provides a reusable atomic-work boundary. Inventory adjustments and their audit events share one transaction-scoped repository context. Demo seeding is opt-in and transactionally repeatable.

## Dependency rules

Allowed direction:

```text
Controller → Service → Repository → Database (single-service Phase 3 operations)
Controller → Facade → Service → Repository → Database (later cross-service workflows)
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

`DatabaseConfig` resolves the application-local database path. `DatabaseManager` creates its parent directory, opens JDBC connections, enables SQLite foreign keys, and configures a busy timeout per connection. `DatabaseHealthCheck` owns `SELECT 1`. Callers use try-with-resources so connections, statements, and result sets close deterministically.

## Decisions deferred beyond Phase 3

- Allocation contracts and formulas
- Request, allocation, and dispatch lifecycles
- The seven required GoF pattern implementations
- Relief-request workflow, allocation, dispatch, notifications, analytics, and reallocation behavior
