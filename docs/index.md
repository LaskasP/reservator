# Reservator Wiki

## Status

This wiki describes the **accepted target MVP design** from the product and architecture interview completed on 2026-06-21. The existing code predates parts of this design. Until a vertical slice is implemented, tests and OpenAPI describe current runtime behavior while these documents describe the intended contract.

## Reading order

1. [Glossary](glossary.md)
2. [Domain model and boundaries](domain/domain-model.md)
3. [Reservation lifecycle](domain/reservation-lifecycle.md)
4. [Availability and scheduling](domain/availability-and-scheduling.md)
5. [Access and audit](domain/access-and-audit.md)
6. [HTTP API contract](api/http-api-contract.md)
7. [Error contract](api/errors.md)
8. [Idempotency](api/idempotency.md)
9. [MVP vertical slices](roadmap/mvp-vertical-slices.md)
10. [Future backlog](roadmap/future-backlog.md)

## Architecture decisions

- [ADR-0001: Use a modular monolith](adr/0001-use-a-modular-monolith.md)
- [ADR-0002: Own the scheduling model](adr/0002-own-the-scheduling-model.md)
- [ADR-0003: Separate identity, catalog, and reservation modules](adr/0003-separate-domain-modules.md)
- [ADR-0004: Use venue time zones and UTC instants](adr/0004-use-venue-time-zones-and-utc-instants.md)
- [ADR-0005: Serialize booking writes with resource-row locks](adr/0005-use-resource-row-locks.md)
- [ADR-0006: Use shared-schema multi-tenancy](adr/0006-use-shared-schema-multi-tenancy.md)

## MVP in one paragraph

Reservator is the reservation domain inside a Spring Boot modular monolith used by web and mobile clients. An organization operates multiple venues; a venue owns independently bookable resources; resources can support multiple data-defined sports. The reservation module owns schedules, availability, holds, confirmations, blocks, audit transitions, and attendance outcomes. Identity comes from OIDC and is mapped to an opaque internal user ID. Pricing, payments, discovery, matchmaking, and outbound notifications remain outside the reservation module.

## Consistency rule

When code, tests, OpenAPI, and this wiki disagree, do not silently choose one. Identify whether the relevant vertical slice is implemented, then update the implementation and documentation together through TDD.
