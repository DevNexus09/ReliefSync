# Business Rules

These rules freeze the first implementation iteration at a behavioral level. Exact thresholds, formulas, role names, and state diagrams will be refined in the phase that introduces each feature.

## Requests and verification

- **BR-001:** A relief request belongs to one affected area and one active disaster event.
- **BR-002:** A request must contain at least one item with a positive requested quantity before submission.
- **BR-003:** Only draft or returned requests may be edited by their submitter.
- **BR-004:** Rejected, cancelled, and fulfilled requests cannot be allocated.
- **BR-005:** Allocation is permitted only after every required verification level has approved the request.
- **BR-006:** Verification depth depends on configured request characteristics such as severity, population affected, total quantity, or special risk.
- **BR-007:** A verifier cannot approve a step for which their role is not authorized.
- **BR-008:** Every approval, return, and rejection is retained in verification history.

## Allocation and reservation

- **BR-009:** All allocation strategies consume the same planning context and return the same allocation-plan structure.
- **BR-010:** Allocation preview is read-only and must not modify inventory.
- **BR-011:** An allocation plan identifies the request item, supplying center, resource, allocated quantity, shortage, and strategy rationale.
- **BR-012:** Multiple centers may contribute to the same requested resource.
- **BR-013:** Partial allocation is allowed, and its unmet quantity remains eligible for later allocation.
- **BR-014:** Only confirmed allocations reserve inventory.
- **BR-015:** Allocation confirmation must reserve all plan lines in one transaction or reserve none of them.
- **BR-016:** Available inventory equals on-hand inventory minus active reservations and must never be negative.
- **BR-017:** An allocation can be reversed only before its resources enter an irreversible dispatch stage.
- **BR-018:** Reversal restores only the quantities still reserved by that allocation.

## Dispatch, delivery, and recovery

- **BR-019:** A dispatch can be created only from a confirmed allocation with reserved inventory.
- **BR-020:** A vehicle cannot be assigned to overlapping active dispatches.
- **BR-021:** Dispatched quantity cannot exceed the allocation's remaining reserved quantity.
- **BR-022:** Delivered quantity cannot exceed dispatched quantity.
- **BR-023:** A successful delivery reduces the request's remaining need by the confirmed delivered quantity.
- **BR-024:** A failed delivery must record a reason before recovery begins.
- **BR-025:** Failure recovery may retry the dispatch, replace its vehicle, select another center, or return eligible quantities to reallocation.
- **BR-026:** Reallocation operates only on recorded unmet or recoverable quantities and cannot duplicate fulfilled quantities.

## Inventory, notifications, and audit

- **BR-027:** Each relief center has at most one inventory balance for a given resource.
- **BR-028:** Every inventory adjustment records its cause and responsible user.
- **BR-029:** Falling below a configured threshold creates a low-stock event.
- **BR-030:** Significant verification, allocation, dispatch, delivery, failure, reallocation, and low-stock events create appropriate in-app notifications.
- **BR-031:** Security-sensitive and inventory-changing commands record actor, action, target, time, and outcome.

## Scope priorities

Must-have features are authentication, roles, disaster events, affected areas, relief centers, resources, inventory, multi-item requests, request State behavior, verification Chain, at least four allocation Strategies, multi-center and partial allocation, reservation, cancellation, vehicles, dispatch, delivery, failure recovery, reallocation, Observer notifications, audit history, search/filtering, at least four reports, and JUnit tests.

Should-have features are balanced allocation, fairness indicators, strategy comparison, dashboard charts, and duplicate detection.

Optional features are donations, resource substitution, CSV/PDF export, and extra analytics.

Out of scope are live GPS, Google Maps, real SMS, payments, machine learning, government-system integrations, networked real-time multi-user operation, a mobile application, and advanced route optimization.
