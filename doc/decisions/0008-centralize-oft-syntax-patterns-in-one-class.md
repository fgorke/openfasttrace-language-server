---
status: accepted
date: 2026-07-03
decision-makers: Felix Gorke
---

# Centralize OFT Syntax Patterns in One Class
`adr~centralize-oft-syntax-patterns-in-one-class~1`

Needs: impl

## Context and Problem Statement

Recognizing OFT syntax in plain text is needed in several places. The pieces are a specification item ID (`req~login~1`), a section keyword line (`Covers:`), a coverage tag (`[impl->req~login~1]`) and a comment marker. Highlighting, completion, diagnostics and location conversion each need this independently. Each one grew its own regular expression for the same syntax. The patterns were defined locally in `OftIdAtPosition`, `OftSemanticTokensProvider`, `OftCompletionContext` and `LocationConverter`. Several were byte-for-byte identical, such as the ID pattern and the section-keyword pattern. That is easy to miss during review. The copies can silently drift apart when one is edited and the others are not.

## Considered Options

* Explicit conversion utilities in a dedicated `OftSyntax` class
* Leave each pattern defined locally where it is used

## Decision Outcome

Chosen option: **a dedicated `OftSyntax` class**. It holds every pattern as a `public static final Pattern` or constant. The four consumers reference it instead of declaring their own copies.

| Pattern | Purpose |
|---|---|
| `SPECIFICATION_ITEM_ID` | A single ID, e.g. `req~login~1` |
| `SPECIFICATION_ITEM_DEFINITION_LINE` | A line consisting solely of an ID |
| `SECTION_KEYWORD_LINE` | A line starting with `Needs:`, `Covers:`, etc. |
| `COVERAGE_TAG` | A well-formed tag with a complete ID target. Used where precision matters, e.g. highlighting |
| `COVERAGE_TAG_LOOSE` | Any bracketed `source->target` span, target not required to be a full ID. Used to tighten a navigation range around a possibly informal tag |
| `ID_CHARACTER_CLASS`, `COMMENT_MARKERS` | Shared constants used while scanning a line character by character |

Two coverage-tag patterns are kept deliberately distinct instead of unified. Highlighting must not mark up malformed tags, so it needs the strict pattern. `LocationConverter` tightens a range around whatever bracket-arrow-bracket span it finds. It does not care whether the target parses as a full ID, so it needs the loose one. Collapsing them would make one of the two use cases wrong.

### Consequences

* Good, because each piece of OFT syntax is defined exactly once. A future change, such as supporting a new artifact-type character, only needs to happen in one place.
* Good, because the duplication that had already made two patterns identical by accident can no longer happen. There is only one copy left to edit.
* Good, because `OftSyntax` gives new code an obvious first place to look for how a piece of syntax is recognized. No more grepping for regex literals across packages.
* Bad, because it introduces a shared class that every syntax-aware component now depends on. That slightly increases coupling between packages that were otherwise independent (`completion`, `highlighting`, `diagnostics`, `index`).
* Bad, because the reach of the class ends at the server. Both clients decide on their own when to ask for completion and need the comment markers for it, so that list exists three times over: in `OftSyntax`, in `OftCompletionConfidence` and in the VS Code extension. A new marker has to be added in all three.

### Confirmation

Existing unit tests for `OftIdAtPosition`, `OftSemanticTokensProvider`, `OftCompletionContext`, `LocationConverter` and `DiagnosticsProvider` keep passing unchanged after the patterns were moved. `OftSyntax` reproduces each pattern's exact behavior rather than altering it. No new tests were added for `OftSyntax` itself. It holds no logic beyond pattern definitions that its consumers already exercise.
