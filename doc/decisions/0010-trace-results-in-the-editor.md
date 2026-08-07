---
status: accepted
date: 2026-08-06
decision-makers: Felix Gorke
---

# Trace Results in the Editor as Diagnostics

## Context and Problem Statement

The server only calls `Oft.importItems()`. It never links, so it never finds out whether an item is covered. Everything it says about link health comes from a hand-written revision check in `DiagnosticsProvider`.

## Considered Options

* Diagnostics through `textDocument/publishDiagnostics`
* A batch-triggered report panel
* Code lenses summarising coverage above each specification item
* A custom LSP extension carrying the full trace tree

## Decision Outcome

Chosen option: **diagnostics**, computed during the regular index rebuild.

`WorkspaceIndexer` extends `importItems()` with `link()` and keeps the linked items. Defects come out of linking, so `trace()` is not needed; it only builds the CLI's report model. Every defect with a location becomes a diagnostic there.

Diagnostics need no client-side code and are the most widely supported part of the protocol. A report panel has no LSP equivalent and would have to be built per client. Code lenses sit on items and leaving a tag that points at a missing item nowhere to go.

Severity comes from `isTransitiveDefect()`, added in OFT 4.8.0. An item broken by itself is a warning, one broken only because something it covers is broken is information. Without that split a single missing link flags every item above it.

### Consequences

* Good, because both clients gain trace feedback with no client-side code, at the position of the defect rather than in a separate report.
* Good, because `DiagnosticsProvider` stops approximating OFT link semantics and the problem view of both IDEs aggregates the findings for free.
* Bad, because diagnostics carry no structure. Coverage rolled up per artifact type needs the code lens layer.
* Bad, because coverage tags in test fixtures are imported as real tags and become real diagnostics.
* Neutral, because results lag behind unsaved edits, as described in ADR-0006.

### Confirmation

Integration tests check that all diagnostics are reported at the right position.

