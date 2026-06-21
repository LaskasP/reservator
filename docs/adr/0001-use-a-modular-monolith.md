# ADR-0001: Use a Modular Monolith

- Status: Accepted
- Date: 2026-06-21

## Context

The application will eventually support web/mobile booking, player discovery, teams, and matchups. The domain is early and does not yet justify distributed deployment and operational complexity.

## Decision

Build one Spring Boot application and PostgreSQL database with explicit identity, catalog, and reservation module boundaries. Modules interact through Java services and in-process events, never internal HTTP.

Use direct service calls for same-transaction invariants, in-process events for optional reactions, and introduce a durable outbox only when a required external side effect appears.

## Consequences

- Simple deployment and cross-module transactions.
- Module discipline must be enforced with architecture tests.
- A future extraction remains possible because repositories/entities are private and public service contracts are narrow.

## Alternatives considered

- Independent microservices now: rejected as premature operational complexity.
- Unstructured monolith: rejected because domain boundaries would erode.
