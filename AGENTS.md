# Reservator

## Structure
- Keep `ReservatorApplication` in `com.skouna.reservator`.
- Organize code by feature under `com.skouna.reservator.<feature>` .

## TDD
- Project is TDD.
- TDD is mandatory: `red -> green -> refactor`.
- Work in vertical slices: one failing behavior test, the minimum code to pass, then refactor.
- Prefer tests through a feature's public behavior; avoid tests coupled to internals.

## Commands
- Windows: `gradlew.bat test`
- macOS/Linux: `./gradlew test`

## Done
- Run tests before claiming work is complete.
