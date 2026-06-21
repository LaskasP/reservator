# ADR-0005: Use Resource-Row Locks for Booking Writes

- Status: Accepted
- Date: 2026-06-21

## Context

Availability is advisory. Concurrent customers and multiple application instances can race to hold the same physical resource and overlapping interval.

## Decision

`ReservationService` starts one transaction and calls a narrow catalog operation that executes `SELECT ... FOR UPDATE` on the resource row and returns an immutable snapshot. Under that lock, reservation checks schedules, blocks, active holds, and bookings before inserting the new command result.

All hold, direct booking, and block write paths follow the same locking protocol. Requests for different resources remain concurrent.

## Consequences

- Locking works across application instances sharing the primary PostgreSQL database.
- The catalog service joins the existing transaction.
- Architecture and concurrency tests must detect bypasses.
- PostgreSQL exclusion constraints may be added later as defense in depth.

## Alternatives considered

- In-memory locks: rejected because they fail across instances.
- Redis locks: rejected as an unnecessary second authority.
- Pre-generated slots: rejected because variable durations, buffers, and overnight windows fit poorly.
- Serializable isolation or a booking queue: deferred as more complex.
