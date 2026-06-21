# HTTP API Contract

## Boundary

Web and mobile clients call the modular monolith over HTTP. Modules call Java services directly; they do not call these endpoints internally.

All public endpoints use a major-version prefix:

```text
/api/v1/...
```

Additive changes remain in `v1`. Breaking request, response, semantic, or error-code changes require a new major version.

## Resource shape

Administration uses explicit organization scoping:

```text
/api/v1/organizations/{organizationId}/venues
/api/v1/organizations/{organizationId}/booking-policy
```

Customer booking APIs remain flatter after discovery selects a venue:

```text
/api/v1/venues/{venueId}/availability
/api/v1/reservations/holds
```

Every path ID is resolved and authorized server-side.

## Commands, not arbitrary status patches

Lifecycle operations are explicit commands:

```text
POST /api/v1/reservations/holds
POST /api/v1/reservations/{id}/complete
POST /api/v1/reservations/{id}/approve
POST /api/v1/reservations/{id}/reject
POST /api/v1/reservations/{id}/cancel
POST /api/v1/reservations/{id}/attendance
```

Do not expose a generic `PATCH status`. Each command owns its validation, authorization, audit, idempotency, and documented errors.

## Time representation

- Availability requests use venue-local dates and a bounded date range.
- Booking and block timestamps are ISO-8601 values with offsets.
- Hold requests use `startsAt + durationMinutes`; the server computes `endsAt`.
- API examples must show offsets.

## Pagination and filtering

MVP list APIs use page-based pagination:

```text
?page=0&size=50&sort=startsAt,asc
```

Enforce a maximum size, deterministic secondary sort by ID, and a whitelist of sortable fields. Cursor pagination is future work.

## Public Java service boundary

The HTTP controller calls the same narrow Java facade used by other modules, for example `ReservationService`. It accepts immutable command records and returns immutable result records. JPA entities are never serialized or returned.

## Error and retry contracts

- Every failed request uses [Problem Details](errors.md).
- Every non-idempotent `POST` command requires [Idempotency-Key](idempotency.md).
- `429` and temporary `503` responses may carry `Retry-After`.
- Clients must not automatically retry validation or business conflicts.

## OpenAPI

Every controller, operation, DTO, status, error, and example is documented with OpenAPI annotations. OpenAPI and `docs/api/` must remain consistent.
