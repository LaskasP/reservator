# API Error Contract

## Universal shape

Every failed API request returns RFC 9457 Problem Details as `application/problem+json`. Successful requests return normal DTOs; errors are never wrapped in `200 OK`.

```json
{
  "type": "https://reservator.example/problems/slot-unavailable",
  "title": "Slot unavailable",
  "status": 409,
  "detail": "This time is no longer available.",
  "instance": "/api/v1/reservations/holds",
  "code": "SLOT_UNAVAILABLE",
  "traceId": "01JXYZ..."
}
```

Clients make decisions using `code`, never by parsing `title` or `detail`. Human-readable strings are stable English fallback text; web/mobile clients own localization.

## HTTP status policy

| Status | Use |
|---:|---|
| 400 | Malformed JSON, unreadable date/time, or request binding failure. |
| 401 | Missing or invalid authentication. |
| 403 | Authenticated caller lacks the required operation-level role. |
| 404 | Entity does not exist or is hidden by organization isolation. |
| 409 | Conflict with current state or idempotency history. |
| 422 | Well-formed content violates field or domain validation. |
| 429 | Rate limit exceeded; may include `Retry-After`. |
| 500 | Unexpected internal failure with sanitized detail. |
| 503 | Temporary service/dependency unavailability; may include `Retry-After`. |

## Validation errors

Return one `VALIDATION_FAILED` problem with all field problems:

```json
{
  "type": "https://reservator.example/problems/validation-failed",
  "title": "Request validation failed",
  "status": 422,
  "detail": "One or more request fields are invalid.",
  "code": "VALIDATION_FAILED",
  "traceId": "01JXYZ...",
  "errors": [
    {
      "pointer": "/startTime",
      "code": "REQUIRED",
      "detail": "Start time is required."
    }
  ]
}
```

## Initial error registry

This table is the canonical target registry. Add a code here before exposing it.

| Code | Status | Meaning and recovery |
|---|---:|---|
| `MALFORMED_REQUEST` | 400 | Correct request encoding or syntax. |
| `UNAUTHENTICATED` | 401 | Obtain or refresh authentication. |
| `FORBIDDEN` | 403 | Caller lacks the required role. |
| `RESOURCE_NOT_FOUND` | 404 | ID is absent or outside caller's organization scope. |
| `SLOT_UNAVAILABLE` | 409 | Refresh availability and choose another option. |
| `INVALID_RESERVATION_STATE` | 409 | Refresh reservation state; requested transition is not allowed. |
| `HOLD_LIMIT_EXCEEDED` | 409 | Complete, cancel, or wait for the existing hold to expire. |
| `HOLD_EXPIRED` | 409 | Create a new hold if the interval is still available. |
| `IDEMPOTENCY_KEY_REUSED` | 409 | Generate a new key because payload differs from the original command. |
| `RESOURCE_BLOCK_CONFLICT` | 409 | Privileged caller resolves returned reservation IDs, then retries. |
| `VENUE_TIME_ZONE_CHANGE_BLOCKED` | 409 | Deactivate venue and resolve future activity. |
| `VALIDATION_FAILED` | 422 | Correct listed field violations. |
| `BOOKING_IN_PAST` | 422 | Choose a future start. |
| `OUTSIDE_OPERATING_HOURS` | 422 | Choose a returned availability option. |
| `DURATION_NOT_ALLOWED` | 422 | Choose an allowed duration. |
| `START_NOT_ALIGNED` | 422 | Use a start returned by availability. |
| `CANCELLATION_WINDOW_CLOSED` | 422 | Customer cutoff passed; contact staff. |
| `RATE_LIMIT_EXCEEDED` | 429 | Wait for `Retry-After` before trying again. |
| `INTERNAL_ERROR` | 500 | Show a generic failure and retain `traceId` for support. |
| `SERVICE_UNAVAILABLE` | 503 | Retry safely according to `Retry-After` and idempotency rules. |

## Role-sensitive details

- Customer conflicts never reveal another reservation's ID, status, owner, or creation time.
- Privileged block commands may include conflicting reservation IDs.
- Cross-organization IDs return the same `404` shape as absent IDs.
- Unexpected failures never expose stack traces, SQL, exception class names, tokens, or internal secrets.

## Implementation

Business/application services throw typed `ApiException` subclasses with stable `ErrorCode`. Controllers do not catch them. `GlobalExceptionHandler` maps exceptions, validation, security failures, and unexpected exceptions into this contract. `ProblemDetail` objects never enter domain logic.
