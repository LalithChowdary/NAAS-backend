# NAAS Delivery Algorithm

The Newspaper Agency Automation System (NAAS) relies on a daily automated batch process to translate hundreds of dynamic customer subscriptions into highly optimized physical routes for the Delivery Fleet.

This document describes the architectural flow of the routing algorithm, highlighting how Google Cloud Fleet Routing maps to our local entities.

## 1. Trigger & Filtering

The process begins exactly once per day (or manually via Administrator trigger at `/api/delivery/admin/generate-schedule`).
The `DeliveryService` scans the database to find all active subscriptions matching the following conditions:
* **Status:** `ACTIVE`
* **Suspensions:** The current date MUST absolutely fall outside of any global pauses or individual Subscription pause dates (`suspendStartDate` → `suspendEndDate`).
* **Item-Level Control:** The engine strips away any individual items internally marked `SUSPENDED` or `REMOVED`. If the subscription becomes completely empty physically, the stop is logically pruned from the map to radically save travel overhead.
* **Idempotency:** It ensures a `DeliveryRecord` hasn't already formally been generated for the target date to securely prevent duplicate manifest generation.

## 2. Dispatch to Fleet Routing Optimization

Any remaining physically valid Subscription stops mapped to coordinates (`lat`, `lng`) are serialized into a heavily compressed payload internally managed by our `FleetRoutingService`.

**Building the Strategy:**
- We instantiate one "Vehicle" block for every configured `DeliveryPerson`. We intentionally DO NOT declare a `startLocation` for the vehicles. This breaks standard hardcoded tracking and shifts the paradigm into full "clustering mode", allowing the mathematical optimizer to natively group geographic regions globally.
- Each customer address becomes a `Shipment` (specifically, a `delivery`) requiring roughly `30s` of service time. 
- Google Maps executes the complex TSP (Traveling Salesperson Problem) across the fleet size.

> [!TIP]
> **Why we omit startLocations:** Hardcoding drivers round-robin sequentially assigns drivers arbitrarily. Omitting it forces Google to logically cluster local nodes, returning perfectly sized logical circles globally!

## 3. Sequence & Dynamic Hub Allocation

The Google Optimization API returns a strict array matching vehicles (Delivery Personnel) with an ordered ledger of visits (`route.visits`).

We execute an overlay processing phase:
1. **Sequence Mapping:** As we recursively iterate through `visits`, we extract the exact index step (`routeSequence = 1, 2, 3..`). This tells the delivery driver the mathematically proven shortest path.
2. **Haversine Hub Snap:** The fleet routing engine clusters routes dynamically. To map these mathematical clusters into the real-world operational domain natively, we read the exact coordinates of the *very first* customer stop on a generated route. We execute a fast `Haversine distance` calculation between that coordinate and all valid system `Hub`s.
3. **Bind Hub:** The structurally closest `Hub` permanently becomes the *Start Hub* (`hubId`) for that specific Delivery Person's current daily Trip!

## 4. State Persistence

The final compiled manifestations are persisted locally into Postgres `DeliveryRecords`.
For each node, we inherently document:
* `delivery_person_id` (Calculated Vehicle)
* `hub_id` (Where the papers must be collected from)
* `route_sequence` (When to hit this stop sequentially)

> [!NOTE]
> If any legacy subscriptions explicitly lack map coordinates, they purposefully bypass the mathematical clustering block entirely and randomly append natively as `PENDING` round-robin items as an operational failsafe guaranteeing the papers physically queue for delivery regardless.
