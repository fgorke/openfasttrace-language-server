---
status: accepted
date: 2026-08-11
decision-makers: Felix Gorke
---

# Project-Wide Trace Problems in IntelliJ

## Context and Problem Statement

The server publishes diagnostics for every file that has a defect, not only for the open ones. VS Code lists all of them in its Problems view. IntelliJ puts them under *Current File* and leaves *Project Errors* empty.

LSP has a request for this case, `workspace/diagnostic`. The IntelliJ client does not implement it yet. [IJPL-189566](https://youtrack.jetbrains.com/issue/IJPL-189566) has been open since then, and JetBrains has confirmed it will come without naming a date.

## Considered Options

* Mirror the published diagnostics into the `ProblemsCollector` of the project
* Implement `workspace/diagnostic` on the server and send the request from the plugin by hand
* Wait for the platform to catch up
* Report the defects through a custom LSP notification

## Decision Outcome

Chosen option: **mirror the published diagnostics**, from inside the plugin.

The plugin overrides `LspClientDescriptor.createLsp4jClient` and wraps the notifications handler. Everything reaches the platform unchanged. `publishDiagnostics` additionally goes to `OftProblemsReporter`, which remembers what it reported per file, compares that against the new list and calls `problemAppeared` or `problemDisappeared` on the collector. That is the same entry point the platform uses for its own analyzers.

### Consequences

* Good, because the server stays untouched. One push, two clients, no capability negotiation.
* Good, because *Project Errors* updates on every index rebuild, including the empty updates that clear what a user has just fixed.
* Bad, because `Problem` is marked experimental in the platform, so the plugin may need adjusting when it changes.
* Bad, because the code turns into dead weight once IJPL-189566 lands and duplicate entries are conceivable then.
* Neutral, because VS Code ignores all of this and keeps working from the push alone.

### Confirmation

Manual, in a sandbox IDE. Defects in files that were never opened have to show up under *Project Errors* and disappear once they are fixed.
