# ReliefSync Scope Baseline

This scope is frozen for the first implementation iteration. A scope change should be discussed by both developers and recorded in the pull request that introduces it.

## Must-have

- Authentication and role-based authorization
- Disaster-event and affected-area management
- Relief-center, resource, and inventory management
- Multi-item relief requests
- State-based request lifecycle
- Dynamic verification chain
- At least four allocation strategies
- Multi-center and partial allocation
- Transactional inventory reservation
- Allocation cancellation and permitted reversal
- Vehicle management
- Dispatch and delivery tracking
- Delivery-failure recovery and reallocation
- Observer-driven in-app notifications
- Audit history
- Search and filtering
- At least four operational reports
- JUnit tests for important business behavior

## Should-have

- Balanced allocation
- Fairness indicator
- Strategy comparison
- Dashboard charts
- Duplicate detection

## Optional

- Donation records
- Resource substitution
- CSV or PDF report export
- Additional analytics

## Out of scope

- Live GPS tracking
- Google Maps integration
- Real SMS delivery
- Payments
- Machine learning
- Government-system integrations
- Networked real-time multi-user operation
- Mobile application
- Advanced route optimization

## Phase 2 boundary

Phase 2 contains the versioned base schema, typed domain models, entity-specific repositories, secure authentication, centralized authorization, opt-in demo seeding, authenticated login/dashboard integration, tests, and documentation. It intentionally contains no operational workflow services or implementations of the seven GoF patterns.

## Phase 3 boundary

Phase 3 adds the six core management modules: disaster events, affected areas, relief centers, resources, inventory, and vehicles. It includes bounded repository search, role-aware navigation, lifecycle-safe activation controls, and transactionally audited stock adjustments. Relief requests, verification, allocation, reservation, dispatch, delivery, notifications, reallocation, analytics, and all seven required GoF pattern implementations remain deferred.

## Phase 4 boundary

Phase 4 adds transactional multi-item relief requests, duplicate warnings and safe merge handling, State-controlled request lifecycle behavior, and a role-specific Chain of Responsibility for Normal, High, and Critical verification. Verification rounds preserve prior decisions after return, revision, and resubmission. Allocation, inventory reservation, dispatch, delivery, Observer notifications, Command reversal, concrete Factory Method creators, the operational Facade, reallocation, and analytics remain deferred.
