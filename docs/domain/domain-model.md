# Domain Model and Module Boundaries

## Scope

The reservation domain owns bookability and booking state. It does not own teammate discovery, player profiles, teams, matchmaking, pricing, payment execution, geospatial discovery, or outbound notifications.

The application is a modular monolith: one Spring Boot deployment and one PostgreSQL database, with explicit Java module boundaries and no internal HTTP calls.

## Modules

### Identity

Owns a minimal internal identity mapping:

- `UserAccount`: stable internal UUID.
- `OidcIdentity`: `(issuer, subject) -> userAccountId`.
- Just-in-time provisioning may create the mapping after the first valid OIDC login.

It owns no passwords. OIDC providers such as Okta authenticate users. The reservation module receives only an opaque `UserId` in an authenticated actor context.

### Catalog

Owns intrinsic, non-booking facts:

- `Organization`
- `Venue`
- `BookableResource`
- Global `Sport`
- `OrganizationMembership`

An organization can operate multiple venues, including venues in different time zones. A staff/admin membership belongs to exactly one organization in the MVP and covers all its venues.

### Reservation

Owns every fact that determines whether, when, and how a resource can be booked:

- `OrganizationBookingPolicy`
- `ResourceSportConfiguration`
- `ScheduleTemplate`
- `ScheduleOverride`
- `VenueBlock`
- `ResourceBlock`
- Availability calculation
- Holds and reservations
- Reservation transition history
- Attendance outcome
- Reservation-related audit behavior

## Dependency direction

```text
identity       catalog
    \           /
     application/security context
              |
         reservation -> catalog public service
```

Reservation may call a narrow catalog Java service and consume immutable snapshots. Catalog does not depend on reservation.

## Persistence boundaries

- JPA entities and repositories are internal to their owning module.
- Public module services accept immutable commands and return immutable results.
- Cross-module persistence references use UUIDs, not JPA associations.
- Database foreign keys may cross module-owned tables to preserve integrity.
- Organization-scoped composite foreign keys prevent cross-tenant relationships.

Example:

```sql
FOREIGN KEY (resource_id, organization_id)
REFERENCES bookable_resource (id, organization_id)
```

## Catalog relationships

```text
Organization
  ├─ Venue (address, coordinates, time zone)
  │    └─ BookableResource (name, description, lifecycle)
  └─ OrganizationMembership (user ID, STAFF or ADMIN)

Sport (global reference data)

Reservation-owned ResourceSportConfiguration
  ├─ resourceId -> BookableResource
  ├─ sportId -> Sport
  ├─ allowed durations
  ├─ start interval
  ├─ post-booking buffer
  └─ schedule restrictions
```

## Lifecycles

- New resources are `INACTIVE`.
- Admins configure bookability and explicitly activate a resource.
- Activation validates that at least one complete resource-sport configuration exists.
- Catalog entities with history are deactivated rather than hard-deleted.
- Existing reservations remain addressable after deactivation or renaming.

## Venue location and time zone

Catalog owns canonical structured address, latitude, longitude, IANA time zone, and active state. Discovery may index these values but remains outside reservation.

Changing a venue time zone is a guarded command:

1. Admin authorization is required.
2. Venue and resources are locked in one transaction.
3. Venue must be inactive.
4. No future holds, reservations, or blocks may exist.
5. A blocked change returns `409 VENUE_TIME_ZONE_CHANGE_BLOCKED`.
6. A successful change is audited.

## Organization provisioning

Creating an organization and its first admin is an internal platform process. Organization admins may add or remove already-provisioned `STAFF` users. Admin-role changes remain internal during the MVP.
