# Access, Tenancy, and Audit

## Authentication and internal identity

- OIDC providers authenticate users and issue access tokens.
- The security/identity layer resolves `(issuer, subject)` to a stable internal `UserId`.
- Reservation receives an authenticated actor context and never handles passwords, profiles, token parsing, or identity-provider entities.
- A `Customer` role is not an organization membership; customers may reserve across organizations.

## Organization roles

An internal user may hold `STAFF` or `ADMIN` membership in one organization in the MVP. That membership covers every venue in the organization.

| Capability | Customer | Staff | Admin |
|---|---:|---:|---:|
| View availability | Yes | Yes | Yes |
| Manage own reservation | Yes | Yes | Yes |
| Create direct or guest booking | No | Yes | Yes |
| Approve, reject, or cancel organization bookings | No | Yes | Yes |
| Record attendance | No | Yes | Yes |
| Create resource blocks | No | Yes | Yes |
| Create venue-wide blocks | No | No | Yes |
| Configure resources, sports, schedules, and policies | No | No | Yes |
| Manage staff membership | No | No | Yes |
| View complete organization audit | No | Limited | Yes |

Organization admins may add/remove an already-provisioned `STAFF` user. Organization creation, initial admin creation, and admin-role changes are internal platform operations.

## Tenant isolation

The MVP uses shared-schema multi-tenancy:

- Organization-scoped rows carry `organization_id`.
- Authorization derives organization membership from the authenticated user.
- Admin/staff paths may contain an organization ID, but request data never grants access.
- Every organization-scoped repository query includes `organization_id`.
- Another organization's entity is reported as `404 RESOURCE_NOT_FOUND`, not `403`.
- Composite foreign keys prevent cross-organization relationships.
- PostgreSQL row-level security is future hardening, not the MVP authority.

## Audit versus observability

Keep three concerns distinct:

- **Audit:** who changed what and when.
- **Operations:** pending approvals, upcoming/past reservations, blocks, cancellations, and attendance follow-up.
- **Technical telemetry:** logs, traces, database health, and error rates for platform operators/developers.

Business analytics are not part of the MVP.

## Audited actions

Write audit evidence in the same transaction as every privileged mutation:

- direct booking, approval, rejection, cancellation, and attendance
- schedule template and override changes
- venue and resource blocks
- organization policy changes
- venue/resource/sport configuration changes
- resource activation/deactivation
- membership changes
- guarded venue time-zone changes

Each audit entry contains actor, organization, action, target type/ID, timestamp, correlation ID, and safe before/after data. Never record access tokens, secrets, stack traces, or unnecessary personal data.

## Audit visibility

- Admins see all audit activity for their organization.
- Staff see reservation and resource-block activity needed for operations.
- Customers do not access the audit log; they receive customer-safe reservation history.
- Audit retention is indefinite for the MVP. Retention and archival policies are future work.

## Operational reservation filters

Staff/admin reservation lists support server-side filtering by start range, status, venue, resource, sport, reserved-for ID, created-by actor, and attendance outcome. Page-based pagination is used initially with a maximum page size and whitelisted sort fields.
