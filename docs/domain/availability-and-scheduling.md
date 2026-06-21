# Availability and Scheduling

## Ownership

Reservation owns recurring schedules, dated overrides, resource-sport restrictions, blocks, and availability. It uses catalog snapshots for active organization, venue, resource, sport, and venue time-zone facts.

## Weekly schedules

- Recurring hours use organization-scoped named `ScheduleTemplate` records.
- Multiple non-overlapping windows are allowed on one weekday.
- Resources and resource-sport configurations may reuse templates.
- Editing a shared template intentionally affects every attached target.
- Existing reservations are never silently cancelled by a schedule change.

## Dated overrides

- A dated override fully replaces the recurring windows for its target and local date.
- It may close the target or provide one or more replacement windows.
- A resource override affects every sport on that resource.
- A resource-sport override affects only that sport.
- Existing reservations are grandfathered; an admin receives affected reservation information and resolves them explicitly.

## Blocks

- `ResourceBlock` represents physical unavailability across every sport on one resource.
- `VenueBlock` represents physical unavailability across every resource in one venue.
- Staff may create resource blocks; only admins create venue-wide blocks.
- Block creation is rejected when active reservations overlap it.
- Privileged errors return conflicting reservation IDs; customer errors never expose them.

## Time model

- Every venue owns an IANA zone ID such as `Europe/Athens`.
- Schedule rules are expressed in venue-local dates and times.
- Reservations and blocks store UTC instants using `TIMESTAMP WITH TIME ZONE`.
- Public timestamps include an offset.
- Reservations snapshot the venue zone ID used when created.
- A window crossing midnight carries an explicit `endDayOffset` of `0` or `1`.
- All intervals are half-open: `[start, end)`.

### DST behavior

- Nonexistent local starts during a spring-forward gap are omitted.
- Repeated local starts during a fall-back overlap become two distinct options with different offsets.
- A hold submits the exact offset returned by availability.

## Resource-sport configuration

Each active configuration defines:

- resource ID and sport ID
- allowed duration values
- start interval
- post-booking buffer, defaulting to zero
- optional recurring sport restriction template
- optional dated sport overrides

The customer books the whole physical resource. A booking blocks every sport on it. Buffers extend the blocking interval but not the customer-visible booked duration.

## Effective availability algorithm

For each requested venue-local date:

1. Verify organization, venue, resource, sport, and resource-sport configuration are active.
2. Resolve resource windows from the dated override or recurring template.
3. Resolve sport restrictions from the dated override or recurring template.
4. Intersect resource and sport windows.
5. Generate candidate starts using the resource-sport start interval.
6. Test every allowed duration and its post-booking buffer.
7. Remove candidates overlapping venue blocks, resource blocks, active holds, pending approvals, or confirmed reservations.
8. Return each valid start with the durations valid at that start.

Customer responses contain available options only; they do not reveal why another time is unavailable. Staff/admin operational APIs may expose authorized causes.

## Availability query

The external discovery flow selects a venue using client location outside reservation. Reservation then accepts a bounded venue-level query filtered by sport and local date range. The target maximum range is 14 days per request.

Conceptual response:

```json
{
  "venueId": "...",
  "sportId": "...",
  "resources": [
    {
      "resourceId": "...",
      "starts": [
        {
          "startsAt": "2026-07-15T18:00:00+03:00",
          "allowedDurationsMinutes": [60, 90]
        }
      ]
    }
  ]
}
```

Availability is advisory. Hold creation revalidates under a database resource lock.

## Hold input

```json
{
  "resourceId": "...",
  "sportId": "...",
  "startsAt": "2026-07-15T18:00:00+03:00",
  "durationMinutes": 90
}
```

The server validates duration and offset and computes `endsAt`.
