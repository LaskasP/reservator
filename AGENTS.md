# Reservator

## Structure
- Keep `ReservatorApplication` in `com.skouna.reservator`.
- The target modular-monolith modules are `identity`, `catalog`, and `reservation`.
- Organize code by feature under `com.skouna.reservator.<module>.<feature>`.
- A module exposes a narrow Java service/facade and immutable command/result records.
- Keep JPA entities and repositories internal to their owning module.
- Reference entities in another module by ID; do not create cross-module JPA relationships.
- Other modules call public Java services directly. Do not make HTTP calls inside the monolith.

## TDD
- Project is TDD.
- TDD is mandatory: `red -> green -> refactor`.
- Work in vertical slices: one failing behavior test, the minimum code to pass, then refactor.
- Prefer tests through a feature's public behavior; avoid tests coupled to internals.
- Every `@Test` method must have a `@DisplayName` in GIVEN/WHEN/THEN format, e.g. `@DisplayName("GIVEN an available slot WHEN a user holds it THEN status is HELD with an expiry time")`.
- Keep fast H2 behavior tests and add PostgreSQL integration tests for Liquibase, locking, concurrency, and PostgreSQL-specific safeguards.
- Architecture tests must enforce module boundaries and prevent repository/entity access from other modules.

## Exceptions
- All custom exceptions live in `com.skouna.reservator.exception`.
- Base class: `ApiException extends RuntimeException` with an `ErrorCode` enum field.
- One concrete exception per HTTP status: `ResourceNotFoundException` (404), `BadRequestException` (400), `ConflictException` (409).
- Add new status codes by creating a new subclass of `ApiException`.
- Error codes are defined in the `ErrorCode` enum; add new entries as needed.
- `GlobalExceptionHandler` maps `ApiException` subclasses to `ProblemDetail` responses. Do not throw Spring's `ErrorResponseException`.

## Database Migrations
- Liquibase changesets use SQL format (`--liquibase formatted sql`), not YAML.
- Files live in `src/main/resources/db/changelog/` and are included from `db.changelog-master.yaml`.
- Use `TIMESTAMP WITH TIME ZONE` instead of `TIMESTAMPTZ` for H2 compatibility in tests.

## Commands
- Windows: `gradlew.bat test`
- macOS/Linux: `./gradlew test`

## Documentation
- Always add OpenAPI annotations (`@Tag`, `@Operation`, `@Schema`) with descriptions and examples when creating or modifying controllers and DTOs.
- Start at `docs/index.md` before changing domain behavior.
- Use the canonical terms in `docs/glossary.md` in code, tests, OpenAPI, and documentation.
- `docs/domain/` defines target MVP behavior; `docs/adr/` records why consequential decisions were made.
- `docs/roadmap/mvp-vertical-slices.md` distinguishes planned behavior from implemented behavior.
- Register every public API error code in `docs/api/errors.md` before using it.
- Update the relevant domain document and ADR when a decision changes.

## Git
- Never commit. The user will commit manually.

## Done
- Run tests before claiming work is complete.
