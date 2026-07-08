---
status: accepted
date: 2026-05-13
decision-makers: Felix Gorke
---

# Use MADR for Architectural Decision Records

## Context and Problem Statement

Decisions made while building this language server need to be written down close to the code. They have to be easy to update. Reading them should not need extra tooling.

## Considered Options

* MADR 4.0
* Plain free-text decision log (no fixed template)
* No written ADRs, decisions only in commit messages

## Decision Outcome

Chosen option: **MADR 4.0**. It is a lightweight Markdown format that lives in the repo. GitHub renders it without plugins.

Records live in `doc/decisions/` with sequential four-digit prefixes (`0001-`, `0002-`, …).

### Consequences

* Good, because decisions sit next to the code. They get reviewed in the same pull requests.
* Good, because no external tool is needed to read or write them.
* Bad, because someone still has to remember to write one when a decision is made.
