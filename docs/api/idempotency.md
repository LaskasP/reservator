# Idempotency Contract

## Purpose

An idempotency key identifies one intended command rather than one network attempt. A client that loses a response can retry without creating a second hold, booking, approval, rejection, or block.

## Required operations

Every non-idempotent `POST` command requires:

```http
Idempotency-Key: <client-generated UUID>
```

This includes hold creation, checkout completion, staff direct booking, resource/venue block creation, approval, rejection, cancellation, and attendance commands.

## Behavior

- First use claims the key and runs the command.
- Same scoped key plus identical payload returns the original status and result.
- Same scoped key plus different payload returns `409 IDEMPOTENCY_KEY_REUSED`.
- A new intended action uses a new key.
- Authentication, authorization, and domain overlap rules still apply.

Scope keys by authenticated actor, organization, operation, and key value. Store a canonical request fingerprint and original result in a separate idempotency table; a single column on `reservation` is insufficient.

## Transaction and concurrency

Claim the key and perform the command in a coordinated transaction. Concurrent attempts with the same key must converge on one stored result. Do not finalize transient infrastructure failures that are safe to retry. Malformed or unauthenticated requests are rejected before command execution and do not claim the key.

## Retention

Retain records for a configurable 24-hour MVP window:

```properties
reservator.idempotency.retention=24h
```

Bind it to a typed, positive `Duration`, document it in configuration metadata, allow environment override, and clean expired records in the background. Future external callbacks may define a longer policy.
