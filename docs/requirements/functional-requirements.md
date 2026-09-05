# Functional Requirements

## Access and identity

- **FR-001:** A registered user can sign in with valid credentials.
- **FR-002:** The system restricts operations according to the signed-in user's role.
- **FR-003:** An administrator can manage user accounts and roles.

## Disaster context and affected areas

- **FR-004:** An authorized user can create, update, view, search, and close a disaster event.
- **FR-005:** An authorized user can register and update an affected area within a disaster event.
- **FR-006:** The system records an affected area's severity, affected population, location, and current status.

## Relief centers, resources, and inventory

- **FR-007:** An authorized user can manage relief centers and their operational status.
- **FR-008:** An authorized user can manage resource definitions and measurement units.
- **FR-009:** A relief-center manager can record inventory receipts and adjustments.
- **FR-010:** The system shows available, reserved, and dispatched quantities separately.
- **FR-011:** The system detects low stock after an inventory-changing operation.

## Relief requests and verification

- **FR-012:** An area coordinator can create a multi-item relief request for an affected area.
- **FR-013:** A request can be edited while it is a draft or has been returned for correction.
- **FR-014:** An area coordinator can submit a valid request for verification.
- **FR-015:** The system determines the required verification levels from the request's risk and scope.
- **FR-016:** An authorized verifier can approve, return, or reject a request at the assigned verification level.
- **FR-017:** The system retains every verification decision with actor, time, level, comments, and outcome.

## Allocation and reservation

- **FR-018:** A relief coordinator can preview an allocation for eligible verified requests without changing inventory.
- **FR-019:** The system provides at least four interchangeable allocation strategies.
- **FR-020:** An allocation strategy can source a request item from more than one relief center.
- **FR-021:** The system supports partial allocation when total eligible stock is insufficient.
- **FR-022:** The allocation preview shows allocated and unmet quantities by request item and relief center.
- **FR-023:** An authorized coordinator can confirm an allocation and reserve its inventory transactionally.
- **FR-024:** The system prevents reservations from making available inventory negative.
- **FR-025:** An authorized coordinator can cancel an eligible allocation and restore its reserved inventory.
- **FR-026:** The system retains unmet need for later reallocation.
- **FR-027:** The system can reallocate unmet need after new stock, allocation cancellation, or a recoverable delivery failure.

## Vehicles, dispatch, delivery, and recovery

- **FR-028:** An authorized user can manage vehicles and their availability.
- **FR-029:** A coordinator can assign suitable available transport to a confirmed allocation.
- **FR-030:** An authorized user can create and start a dispatch for reserved resources.
- **FR-031:** An authorized user can update a dispatch through its permitted lifecycle.
- **FR-032:** An authorized user can record delivered quantities and confirm delivery.
- **FR-033:** An authorized user can record a delivery failure and its reason.
- **FR-034:** The system supports retry, vehicle replacement, center replacement, or reallocation after failure, where permitted.

## Notifications and auditability

- **FR-035:** The system creates in-application notifications for significant workflow events.
- **FR-036:** A user can view and mark their notifications as read.
- **FR-037:** The system records auditable entries for security-sensitive and inventory-changing operations.
- **FR-038:** An authorized user can inspect workflow history for a request, allocation, or dispatch.

## Search and analytics

- **FR-039:** Users can search and filter disaster events, affected areas, requests, centers, inventory, allocations, and dispatches using relevant criteria.
- **FR-040:** The dashboard summarizes active disasters, pending verification, unmet demand, low stock, and active dispatches.
- **FR-041:** The system reports requested, allocated, delivered, and unfulfilled resources by affected area.
- **FR-042:** The system reports relief-center inventory utilization.
- **FR-043:** The system reports dispatch and delivery performance, including failures.
- **FR-044:** The system reports allocation fulfillment and fairness information.

## Phase 1 executable foundation

- **FR-045:** The application launches into a placeholder login view.
- **FR-046:** The placeholder login view can navigate to a placeholder dashboard.
- **FR-047:** The dashboard can navigate back to the login view without opening another application window.
- **FR-048:** Application startup verifies the local SQLite connection and reports its version.
