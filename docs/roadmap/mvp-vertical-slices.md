# MVP Vertical Slices

## Delivery rule

Every slice follows mandatory TDD: one failing public-behavior test, minimum code to pass, then refactor. Every `@Test` has a GIVEN/WHEN/THEN `@DisplayName`. Update domain docs, OpenAPI, and error registry in the slice that changes them.

## First executable milestone

The first tracer milestone proves one complete path:

1. Internally provision one organization and admin.
2. Admin creates a venue, resource, sport configuration, and weekly schedule.
3. An authenticated customer views venue availability.
4. Customer places an idempotent hold.
5. Customer completes an auto-confirmed reservation.
6. A concurrent customer receives `409 SLOT_UNAVAILABLE`.
7. State transitions and privileged setup changes appear in audit history.

## Suggested slice order

### Slice 1: Architecture and persistence baseline

- Introduce identity, catalog, and reservation package boundaries.
- Add architecture tests.
- Replace the undeployed legacy schema with target SQL Liquibase changesets.
- Establish RFC 9457 error handling and initial error codes.

### Slice 2: Internal provisioning and catalog activation

- Internal organization/initial-admin provisioning.
- Venue, global sport, and inactive resource creation.
- Resource activation validation.
- OIDC identity mapping and organization membership checks.

### Slice 3: Weekly availability

- Schedule templates, resource-sport configuration, allowed durations, intervals, and buffers.
- Venue-level availability by sport and bounded date range.
- Multiple daily and overnight windows, half-open intervals, and DST tests.

### Slice 4: Idempotent holds under contention

- Separate idempotency persistence and 24-hour configuration.
- Catalog resource-row lock joined to reservation transaction.
- One-active-hold rule and logical expiry.
- Concurrent integration test against H2 and PostgreSQL.

### Slice 5: Automatic confirmation and cancellation

- Explicit complete/cancel commands.
- Organization policy snapshots and cancellation deadline.
- Transition history and customer-safe errors.

### Slice 6: Manual approval

- `PENDING_APPROVAL`, approve/reject, approval timeout, and operational filters.
- Stable reason codes and notes.

### Slice 7: Staff operations and blocks

- Direct user/guest booking.
- Resource and venue blocks with privileged conflict IDs.
- Staff/admin permission matrix.

### Slice 8: Overrides and guarded administration

- Resource and resource-sport dated replacement overrides.
- Schedule-change impact warnings.
- Guarded venue time-zone change.

### Slice 9: Audit and attendance

- Transactional privileged audit entries and role-sensitive audit API.
- Past-reservation follow-up and `UNRECORDED`/`ATTENDED`/`NO_SHOW` outcomes.

### Slice 10: Verification and load correctness

- Full H2 behavior suite.
- PostgreSQL migration/locking suite.
- Hot-slot, distributed-resource, idempotency-storm, and expiry-boundary scenarios.

## Definition of done per slice

- Failing behavior test observed before implementation.
- Full tests pass.
- PostgreSQL tests pass when database behavior is involved.
- OpenAPI annotations and examples are current.
- Error codes are registered.
- Relevant wiki and ADR links are updated.
- No cross-module entity or repository access is introduced.
