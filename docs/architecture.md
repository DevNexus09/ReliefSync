# Architecture

## Layering

```text
JavaFX panes (com.reliefsync.ui)
        ↓
ReliefOperationFacade (workflows) + services (com.reliefsync.service)
        ↓
State / Chain of Responsibility / Strategy (pattern packages)
        ↓
Repositories (com.reliefsync.repository)
        ↓
Database singleton → SQLite (data/reliefsync.db)
```

Dependency rules:

- UI never contains SQL and never talks to repositories for workflow actions — only to the facade and services.
- Services contain business rules and transactions; they delegate lifecycle legality to State, verification depth to the Chain, and distribution policy to Strategy.
- Repositories contain all SQL (parameterized, bounded) and row mapping; they hold no business rules.
- Pattern packages are pure Java with no JavaFX or JDBC imports, so all business logic is unit-testable without a UI or database.

## ER diagram

```mermaid
erDiagram
    USERS ||--o{ RELIEF_REQUESTS : creates
    USERS ||--o{ VERIFICATIONS : decides
    AFFECTED_AREAS ||--o{ RELIEF_REQUESTS : "requests relief"
    RELIEF_REQUESTS ||--|{ REQUEST_ITEMS : contains
    RELIEF_REQUESTS ||--o{ VERIFICATIONS : "verified by rounds"
    RELIEF_REQUESTS ||--o{ ALLOCATIONS : "fulfilled by"
    RELIEF_REQUESTS ||--o{ STATUS_HISTORY : "audited by"
    RESOURCES ||--o{ REQUEST_ITEMS : "requested as"
    RESOURCES ||--o{ INVENTORY : "stocked as"
    RESOURCES ||--o{ ALLOCATIONS : "allocated as"
    RELIEF_CENTERS ||--o{ INVENTORY : holds
    RELIEF_CENTERS ||--o{ ALLOCATIONS : supplies

    USERS { int id PK "username UNIQUE, role, password_hash (PBKDF2)" }
    AFFECTED_AREAS { int id PK "name UNIQUE, district, population, severity 1-5, active" }
    RELIEF_CENTERS { int id PK "name UNIQUE, location, capacity, active" }
    RESOURCES { int id PK "name UNIQUE, unit, low_stock_threshold, active" }
    INVENTORY { int id PK "center_id FK + resource_id FK UNIQUE, quantity >= 0" }
    RELIEF_REQUESTS { int id PK "area_id FK, priority, status, created_by FK" }
    REQUEST_ITEMS { int id PK "request_id FK, resource_id FK, qty_requested > 0, qty_allocated" }
    VERIFICATIONS { int id PK "request_id FK, round_role, verifier_id FK, approved, comment" }
    ALLOCATIONS { int id PK "request_id FK, center_id FK, resource_id FK, quantity > 0, strategy" }
    STATUS_HISTORY { int id PK "request_id FK, from_status, to_status, changed_by, changed_at" }
```

Constraints are enforced in the schema: foreign keys (with `PRAGMA foreign_keys=ON`), UNIQUE names, CHECK ranges on severity/quantities, and `UNIQUE(center_id, resource_id)` for inventory upserts. Migrations are versioned in `schema_version` and applied automatically at startup.

## Request lifecycle

```mermaid
stateDiagram-v2
    [*] --> DRAFT : create draft
    DRAFT --> SUBMITTED : submit
    SUBMITTED --> SUBMITTED : round approved,\nmore rounds pending
    SUBMITTED --> VERIFIED : final round approved
    SUBMITTED --> REJECTED : any round rejected
    VERIFIED --> ALLOCATED : allocate (Strategy,\nstock reserved)
    ALLOCATED --> DISPATCHED : dispatch
    DISPATCHED --> DELIVERED : confirm delivery
    DRAFT --> CANCELLED : cancel
    SUBMITTED --> CANCELLED : cancel
    VERIFIED --> CANCELLED : cancel
```

Verification rounds by priority: NORMAL → Area Coordinator; HIGH → + Relief Coordinator; CRITICAL → + Administrator. The administrator may act for any round.
