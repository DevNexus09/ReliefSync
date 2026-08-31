# ReliefSync — Disaster Relief Resource Coordination System

ReliefSync is a desktop-based disaster relief coordination application designed to support the management and distribution of emergency resources during disasters such as floods, cyclones, severe waterlogging, and other emergency situations.

The system focuses on coordinating **affected areas, relief centers, volunteers, coordinators, supplies, allocations, transport, and deliveries** through structured workflows rather than functioning as a simple CRUD application.

---

## Core Idea

During a disaster, multiple affected areas may request food, water, medicine, shelter materials, and other emergency supplies while available resources remain limited across several relief centers.

ReliefSync helps determine:

- Which affected areas should receive resources first
- Which relief centers should provide the required resources
- How limited resources should be distributed
- How relief requests should be verified
- How allocated resources should be dispatched and delivered
- How shortages and delivery progress should be monitored

The core problem addressed by the system is:

> How should limited relief resources be distributed among multiple affected areas according to urgency, availability, distance, and actual need?

---

## Main Workflow

```text
Affected Area
     ↓
Relief Request
     ↓
Verification
     ↓
Resource Allocation
     ↓
Relief Center
     ↓
Dispatch
     ↓
Delivery
     ↓
Confirmation
