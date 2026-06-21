# ADR-0006: Use Shared-Schema Multi-Tenancy

- Status: Accepted
- Date: 2026-06-21

## Context

The application serves multiple organizations, but separate databases or schemas would multiply provisioning, migrations, connections, and operations before scale requires them.

## Decision

Use one PostgreSQL schema. Organization-scoped rows carry `organization_id`; authorization and repository methods always scope by organization. Composite foreign keys ensure related rows share the same organization. Inaccessible cross-organization IDs return `404`.

## Consequences

- Migrations and operations remain simple.
- Tenant-scoping bugs must be prevented by service patterns, architecture tests, integration tests, and database constraints.
- PostgreSQL row-level security remains a future defense-in-depth option.

## Alternatives considered

- Schema per organization: rejected as premature operational overhead.
- Database per organization: rejected as significantly heavier isolation than the MVP requires.
