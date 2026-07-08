---
status: accepted
date: 2026-05-13
decision-makers: Felix Gorke
---

# Workspace Indexing Strategy: Full Import on Start, File-Level Re-Import on Save

## Context and Problem Statement

The LSP server needs an up-to-date index of OFT spec items and coverage tags. That index answers definition, reference, hover and diagnostic requests quickly. `Oft.importItems()` does a full workspace scan. It is not built for incremental updates. So the strategy has to balance correctness against performance.

## Considered Options

* Full re-import on every `didChange` notification
* Full re-import on `didSave` + `workspace/didChangeWatchedFiles`, debounced
* Intra-file incremental update (re-parse only the changed file on `didChange`)

## Decision Outcome

Chosen option: **full import on server start, then file-level re-import on `didSave` and `workspace/didChangeWatchedFiles`, debounced 300 ms**. It is the simplest correct approach for M1. It skips the complexity of tracking inter-file dependencies incrementally.

A `didChange` (keystroke-level) event does **not** trigger a re-import. Only saves do.

This decision covers only the **OFT workspace index**: spec items and coverage links across the whole project. That is what `Oft.importItems()` rebuilds. It is genuinely expensive to redo on every keystroke. It does **not** mean the server ignores unsaved edits. The current text of every open document is still tracked in memory. It is updated on every `didChange` (`req~live-document-buffer~1`). So requests that only need the text of the file being edited see live content. That covers hover, definition, semantic tokens and especially completion, which almost always runs on text the user has not saved yet. Only cross-file concerns that depend on the *index* lag behind until the next save. An example is whether a given ID still exists workspace-wide.

### Consequences

* Good, because the index always matches the saved state of the workspace.
* Good, because the implementation is simple. It calls `Oft.importItems()` on the full workspace path and rebuilds the internal maps.
* Good, because separating "live document text" from "workspace index" keeps the expensive part save-gated while the cheap part stays live. Completion would be unusable if it only saw saved content.
* Neutral, because the *index* lags behind unsaved edits. A newly typed spec item ID does not resolve workspace-wide until saved. That matches most build-tool-backed language servers.
* Bad, because a large workspace re-imports everything on every save, not just the changed file. Worth revisiting if profiling later shows this is too slow.
* Bad, because the debounce adds up to 300 ms of deliberate delay before the index catches up.

### Confirmation

Integration tests verify that after a file save, the next definition request returns the updated location.

## More Information

A possible follow-up is file-scoped re-import: re-parse only the saved file and merge it into the existing index. That needs a better understanding of OFT's import pipeline than we have now.
