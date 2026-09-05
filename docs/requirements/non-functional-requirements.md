# Non-Functional Requirements

## Compatibility and operation

- **NFR-001:** The project must compile to Java 21 bytecode.
- **NFR-002:** JavaFX, SQLite JDBC, testing, and build-tool versions must be pinned in Maven.
- **NFR-003:** The application must run offline after Maven dependencies are available locally.
- **NFR-004:** SQLite data must persist across normal application restarts.
- **NFR-005:** The application must not rely on hard-coded absolute paths.

## Architecture and maintainability

- **NFR-006:** JavaFX controllers must contain no SQL or JDBC calls.
- **NFR-007:** Repositories must contain no JavaFX or authorization logic.
- **NFR-008:** Allocation algorithms must be testable without starting JavaFX.
- **NFR-009:** The seven required design patterns must solve distinct documented design problems.
- **NFR-010:** Scene replacement must be centralized; controllers must not construct primary application stages.
- **NFR-011:** Operational dependencies must follow the documented layer direction.

## Correctness and reliability

- **NFR-012:** Invalid lifecycle transitions must be rejected without partial state changes.
- **NFR-013:** Inventory-changing operations must be transactional.
- **NFR-014:** Database connections, statements, and result sets must be closed deterministically.
- **NFR-015:** Allocation confirmation must remain consistent if any reservation step fails.
- **NFR-016:** Quantities stored or calculated by the system must never become negative.
- **NFR-017:** Audit records for completed sensitive operations must not be silently overwritten.

## Security and usability

- **NFR-018:** Passwords must be stored using JDK PBKDF2 with a unique salt and an appropriate work factor.
- **NFR-019:** Authorization must be enforced in application services, not only by hiding interface controls.
- **NFR-020:** Validation errors must explain how the user can correct the input.
- **NFR-021:** Screens must remain usable at the application's minimum supported window size.

## Quality and collaboration

- **NFR-022:** Important business rules, strategies, state transitions, verification escalation, and inventory transactions must have automated tests.
- **NFR-023:** `mvn clean test` must succeed before a feature is merged.
- **NFR-024:** Both developers must contribute through identifiable commits and reviewed pull requests.
- **NFR-025:** Generated files, local databases, and IDE-specific settings must not be committed.
