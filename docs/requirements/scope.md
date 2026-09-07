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
