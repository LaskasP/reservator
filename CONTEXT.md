# Reservator Context

Reservator is a modular-monolith reservation application whose target MVP supports organizations, venues, independently bookable resources, multiple data-defined sports, configurable availability, holds, confirmations, privileged operations, audit history, and attendance follow-up.

## Canonical domain context

- Start with `docs/index.md` for the wiki map and implementation-status warning.
- Use `docs/glossary.md` for canonical vocabulary.
- Read the relevant behavioral contract under `docs/domain/`.
- Read relevant accepted decisions under `docs/adr/`.
- Use `docs/roadmap/mvp-vertical-slices.md` to distinguish target behavior from implemented behavior.
- Use `docs/roadmap/future-backlog.md` for explicitly deferred work.

The target modules are `identity`, `catalog`, and `reservation`. The current code predates parts of the accepted target design, so do not silently treat a wiki decision as already implemented.
