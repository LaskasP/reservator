# Domain Glossary

Use these terms consistently in code, tests, APIs, and documentation.

| Term | Meaning |
|---|---|
| Organization | A sports club, commercial operator, municipality, or other owner of venues. Replaces the legacy term `Vendor`. |
| Venue | A physical location operated by one organization. It owns an address, coordinates, and IANA time zone. |
| Bookable resource | One independently reservable physical unit, such as a court or pitch. Replaces the canonical legacy term `Court`. UI copy may use sport-specific words. |
| Sport | Global reference data identified by a stable code, such as `padel` or `football`. It is data, not a Java enum. |
| Resource-sport configuration | Reservation-owned configuration saying that a resource supports a sport, including durations, start interval, buffer, schedules, and active state. |
| Schedule template | An organization-scoped reusable weekly set of availability windows. |
| Schedule override | Replacement availability for one target on one local date. It fully replaces that target's recurring windows for the date. |
| Venue block | Physical unavailability affecting every resource and sport in a venue. |
| Resource block | Physical unavailability affecting every sport on one resource. |
| Booking policy | Organization-wide reservation rules: hold TTL, booking notice, advance window, confirmation mode, cancellation cutoff, and approval timeout. |
| Hold | A short-lived exclusive claim on a resource and interval while a customer completes the booking flow. |
| Reservation | The durable aggregate recording ownership, resource, sport, interval, lifecycle status, policy outcomes, and attendance outcome. |
| Customer reference | A typed reference: `USER` plus an internal user ID, or `GUEST` plus an external opaque guest ID. |
| Actor | The authenticated internal user who performs a command. |
| Attendance outcome | A separate post-visit value: `UNRECORDED`, `ATTENDED`, or `NO_SHOW`. It is not the reservation lifecycle status. |
| Audit entry | Immutable evidence of a privileged mutation, including actor, organization, action, target, time, correlation ID, and safe before/after data. |
| Idempotency key | A client-generated identifier for one intended command, reused across retries so the command produces one result. |

## Avoided terms

- Do not introduce new domain code using `Vendor`; use `Organization`.
- Do not use `Court` as the generic type; use `BookableResource`.
- Do not use the ambiguous lifecycle status `BOOKED`; use `PENDING_APPROVAL` or `CONFIRMED`.
- Do not use `username` as reservation ownership; use an opaque internal `UserId`.
