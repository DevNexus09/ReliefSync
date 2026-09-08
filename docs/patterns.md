# Design Pattern Justifications

Each pattern below was introduced for a concrete design problem in ReliefSync, not to fill a quota. For each: the problem, why the pattern fits, the alternatives considered, and the future benefit.

## State — `com.reliefsync.state`

**Problem.** A relief request passes through eight lifecycle states (Draft, Submitted, Verified, Rejected, Allocated, Dispatched, Delivered, Cancelled) and every action (submit, verify, allocate, dispatch, deliver, cancel) is legal only in some of them. Encoding this in services would scatter `if (status == …)` checks across the codebase and make illegal transitions easy to miss.

**Solution.** Each status has one state class overriding only its legal transitions; everything else fails with a clear message (`RequestState.deny`). Services ask the state object for the next status and never inspect the status themselves.

**Alternatives considered.** Status checks in every service method (duplicated, error-prone); a transition table map (compact, but cannot attach per-state behavior like "Allocated blocks cancel because stock is reserved").

**Future benefit.** Adding a state (e.g. `PARTIALLY_DELIVERED`) means one new class plus registry entry; existing services stay untouched. The whole transition graph is unit-testable without a database (`RequestStateTest`).

## Chain of Responsibility — `com.reliefsync.verification`

**Problem.** How many human approvals a request needs depends on priority: Normal needs the area coordinator; High adds the relief coordinator; Critical adds the administrator. Each login contributes exactly one decision, and the depth must be easy to change.

**Solution.** One handler per approval round, linked into a chain built per priority (`VerificationChains.forPriority`). A handler forwards past rounds that are already approved, enforces that the actor holds the round's role, and reports whether the chain is complete. The State pattern then maps the outcome to Verified / Rejected / still Submitted.

**Alternatives considered.** A rounds counter on the request (cannot express *who* must decide each round); role checks in the service (buries the policy in procedural code).

**Future benefit.** A new approval level (e.g. district officer for CRITICAL) is one new handler class linked into the chain; the service, UI, and database schema do not change. The policy is unit-testable in isolation (`VerificationChainTest`).

## Strategy — `com.reliefsync.strategy`

**Problem.** There is no single correct way to split limited stock across relief centers: draining the largest stockpile minimizes pickup points, while spreading withdrawals preserves local reserves. The coordinator should choose per situation and see the consequences before committing.

**Solution.** `AllocationStrategy` is a pure planning function (items + stock → planned lines) with two implementations, registered by name. Because strategies never touch the database, the UI previews any strategy safely, and the service applies the chosen plan transactionally.

**Alternatives considered.** One hard-coded algorithm (no choice, no preview); flag parameters on one method (each new policy grows the same function).

**Future benefit.** A distance-aware "nearest center first" strategy is one new class plus a registry line — service, UI combo box, and tests pick it up automatically (`AllocationStrategyTest`).

## Facade — `com.reliefsync.facade.ReliefOperationFacade`

**Problem.** The request workflow spans two services (requests + allocation) and three repositories. Without a boundary, every UI pane would wire and coordinate several services and know which one implements which step.

**Solution.** One facade exposes the whole workflow (draft → verify → allocate → dispatch → deliver) plus the read models the screens need. Panes depend on this single API.

**Alternatives considered.** Direct service access from the UI (workflow knowledge leaks into controllers); merging the services (one oversized class mixing verification and inventory concerns).

**Future benefit.** Deferred features (notifications on status change, reallocation after failed delivery) plug in behind the facade without touching any screen.

## Singleton — `com.reliefsync.db.Database`

**Problem.** A desktop SQLite application needs exactly one owned connection so that `inTransaction` gives every repository call inside a workflow the same transaction, and so migrations run once at startup.

**Solution.** `Database` owns the connection and the transaction helper; `init(url)` allows tests to point the same singleton at a temporary database file.

**Alternatives considered.** A connection pool (needless for a single-user desktop app and complicates SQLite locking); passing a connection through every constructor (ceremony without benefit at this scale).

**Future benefit.** Swapping the storage location, enabling WAL mode, or adding connection-level pragmas happens in one class. Tests already exploit the seam (`ReliefWorkflowIntegrationTest`).

## Repository — `com.reliefsync.repository`

**Problem.** SQL embedded in services or UI makes both untestable and couples every feature to the schema.

**Solution.** One repository per aggregate hides all SQL and row mapping behind typed methods; services contain only business rules.

**Future benefit.** Schema changes stay local to one repository; queries are parameterized and bounded (LIMIT) in one auditable layer.
