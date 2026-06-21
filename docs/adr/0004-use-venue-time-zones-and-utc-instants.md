# ADR-0004: Use Venue Time Zones and UTC Instants

- Status: Accepted
- Date: 2026-06-21

## Context

One organization may operate venues in different time zones. Local schedules need venue meaning, while persisted reservations must remain unambiguous across application instances and DST changes.

## Decision

- Each venue owns an IANA zone ID.
- Availability queries use venue-local dates and rules.
- Reservation/block boundaries persist as UTC instants with `TIMESTAMP WITH TIME ZONE`.
- Public timestamps include offsets.
- Reservations snapshot the venue zone ID used at creation.
- Venue time-zone changes are guarded and require an inactive venue with no future activity.

## Consequences

- DST gaps and overlaps require explicit tests.
- Clients submit exact offsets returned by availability.
- Fixed offsets such as `UTC+2` are not valid venue time zones.

## Alternatives considered

- Organization-level zone: rejected because organizations can have multiple venues.
- Store local date/time only: rejected as ambiguous during DST and cross-zone operation.
