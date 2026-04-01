# Reservator

## Structure
- Keep `ReservatorApplication` in `com.skouna.reservator`.
- Organize code by feature under `com.skouna.reservator.<feature>` .

## TDD
- Project is TDD.
- TDD is mandatory: `red -> green -> refactor`.
- Work in vertical slices: one failing behavior test, the minimum code to pass, then refactor.
- Prefer tests through a feature's public behavior; avoid tests coupled to internals.
- Every `@Test` method must have a `@DisplayName` in GIVEN/WHEN/THEN format, e.g. `@DisplayName("GIVEN an available slot WHEN a user holds it THEN status is HELD with an expiry time")`.

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

## Git
- Never commit. The user will commit manually.

## Done
- Run tests before claiming work is complete.
