# ADR-0002: Own the Scheduling Model

- Status: Accepted
- Date: 2026-06-21

## Context

Reservator needs weekly windows, dated replacements, multiple windows per day, overnight operation, sport restrictions, and physical blocks. General calendar libraries focus on iCalendar exchange or arbitrary recurrence rather than reservation invariants.

## Decision

Implement the scheduling model with `java.time` and reservation-owned database records. Dated overrides replace recurring windows. Reusable schedule templates reduce duplication. Availability is calculated by the reservation module and revalidated transactionally at hold creation.

## Consequences

- Domain semantics remain explicit and testable.
- The application owns DST, overnight, and intersection tests.
- iCal4j may be introduced later for `.ics` import/export, not as the booking authority.

## Alternatives considered

- iCalendar recurrence as the core model: rejected as more general and less aligned with bookability.
- External calendar service: rejected for the MVP.
