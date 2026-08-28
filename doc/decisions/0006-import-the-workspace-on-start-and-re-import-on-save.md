---
status: accepted
date: 2026-05-13
decision-makers: Felix Gorke
---

# Import the Workspace on Start and Re-Import on Save
`adr~import-the-workspace-on-start-and-re-import-on-save~1`

Needs: impl

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

**Excluding build output** Handing the workspace root straight to `Oft.importItems()` also indexed build output. The indexer now does its own file walk. It prunes hidden paths and the common build output names `target`, `build`, `out`, `dist` and `node_modules` at every depth, then hands the surviving files to OFT. Files without a matching importer are skipped by OFT itself, so passing single files is safe. A `.oftignore` file in the workspace root adds further glob patterns.

**Off the message loop** The initial import runs asynchronously and reports itself as a task through `window/workDoneProgress`. Doing it inside the `initialized` notification blocked the editor for as long as the import took, which on a few thousand files is seconds.

### Consequences

* Good, because the index always matches the saved state of the workspace.
* Good, because the implementation is simple.
* Good, because build output stays out of the index. Copied specification files no longer show up as duplicates in completion and navigation.
* Good, because a large workspace no longer freezes the editor while it is indexed. The price is that requests before the first import finishes are answered from an empty index.
* Good, because separating "live document text" from "workspace index" keeps the expensive part save-gated while the cheap part stays live. Completion would be unusable if it only saw saved content.
* Neutral, because the *index* lags behind unsaved edits. A newly typed spec item ID does not resolve workspace-wide until saved. That matches most build-tool-backed language servers.
* Bad, because a large workspace re-imports everything on every save, not just the changed file. Worth revisiting if profiling later shows this is too slow.
* Bad, because the debounce adds up to 300 ms of deliberate delay before the index catches up.

### Confirmation

Integration tests verify that after a file save, the next definition request returns the updated location. A further integration test builds a workspace with a specification copy under `target/` and checks that only the original ends up in the index.

## More Information

A possible follow-up is file-scoped re-import: re-parse only the saved file and merge it into the existing index. That needs a better understanding of OFT's import pipeline than we have now.
