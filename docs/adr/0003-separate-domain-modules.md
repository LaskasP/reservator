# ADR-0003: Separate Identity, Catalog, and Reservation Modules

- Status: Accepted
- Date: 2026-06-21

## Context

An earlier catalog/reservation split placed resource booking configuration in catalog and schedules in reservation, creating a circular dependency.

## Decision

- Identity owns internal user IDs and OIDC issuer/subject mappings.
- Catalog owns organizations, venues, resources, sports, and organization memberships.
- Reservation owns policies, resource-sport booking configuration, schedules, blocks, availability, and reservation lifecycle.
- Cross-module persistence references are IDs, never JPA associations.
- Reservation consumes immutable catalog snapshots through a public Java service.

## Consequences

- Dependency direction remains one-way from reservation to catalog.
- Catalog contains intrinsic facts; reservation contains bookability facts.
- Some use cases spanning modules require an application-level orchestrator in the same transaction.

## Alternatives considered

- Cross-module JPA graphs: rejected because they expose persistence and create tight coupling.
- One module per entity: rejected as too fine-grained.
