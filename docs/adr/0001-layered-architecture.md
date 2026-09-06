# ADR 0001: Layered Architecture

- **Status:** Accepted
- **Date:** 2026-09-06

## Context

ReliefSync must support decision-heavy relief workflows while remaining understandable and maintainable for a two-developer academic project. JavaFX controllers, business rules, pattern implementations, and SQLite access need clear ownership.

## Decision

Use the following dependency direction:

```text
JavaFX View → Controller → Facade → Service/Pattern → Repository → SQLite
```

FXML contains presentation structure only. Controllers translate user actions into application calls. The facade coordinates complete use cases. Services enforce authorization, validation, transactions, and business rules. Pattern components encapsulate the distinct variations they are introduced to solve. Repositories isolate JDBC and persistence mapping.

The Phase 1 application remains non-modular and does not include `module-info.java`. Maven manages JavaFX instead of a manually installed SDK.

## Consequences

- Controllers stay small and contain no SQL.
- Allocation logic can later be tested without JavaFX.
- SQLite can be replaced or tested through repository boundaries.
- Additional classes are introduced only when their layer has real behavior.
- Cross-layer shortcuts are rejected even when they appear faster initially.
