# Future Feature Backlog

These items are explicitly outside the target MVP unless promoted through a new decision and vertical slice.

## Booking experience

- Recurring customer reservation series, materialized as individual occurrences.
- Atomic rescheduling that acquires a replacement before releasing the old interval.
- Waitlists, fair offer windows, and release notifications.
- Customer booking quotas by week, organization, or number of future reservations.
- Composite/divisible resources such as a full pitch and two half-pitches.
- Customer attendance confirmation, ratings, and match feedback.
- Venue-specific staff permissions and multi-organization staff membership.

## Integrations

- Payment and pricing module; reservation continues to store no amount or price snapshot.
- Durable outbox/workflow for required external side effects such as refunds.
- Email, SMS, and push notification module.
- `.ics` export and Google/Outlook calendar synchronization.
- Staff invitation and identity-provider pre-provisioning.

## Discovery and catalog enrichment

- Geospatial venue search/ranking remains outside reservation.
- Resource metadata such as surface, indoor/outdoor, lighting, accessibility, and photos.
- Sport metadata for teams, rules, icons, and matchmaking.

## Reporting and operations

- Business analytics dashboards and utilization reporting.
- Configurable audit retention and archival.
- Cursor pagination for large, rapidly changing feeds.
- Mixed-workload load testing.
- Long-running soak testing.

## Scale and hardening

- Advisory availability cache behind the reservation module.
- PostgreSQL exclusion constraints as defense in depth.
- PostgreSQL row-level security as tenant-isolation defense in depth.
- Alternative deployment/module extraction if scale or team ownership justifies services.
