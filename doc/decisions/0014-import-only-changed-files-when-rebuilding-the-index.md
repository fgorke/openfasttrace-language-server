---
status: accepted
date: 2026-09-07
decision-makers: Felix Gorke
---

# Import Only Changed Files When Rebuilding the Index
`adr~import-only-changed-files-when-rebuilding-the-index~1`

Needs: impl

## Context and Problem Statement

Every save and every file change outside the editor rebuilds the whole index ([ADR 0006](0006-import-the-workspace-on-start-and-re-import-on-save.md)), which re-parses every file in the workspace. Measuring shows:

| workspace | walk | import | link | build |
| --- | --- | --- | --- | --- |
| this repository | 12 ms | 52 ms | 2 ms | 2 ms |
| synthetic, 6000 files | 44 ms | 1170 ms | 4 ms | 6 ms |

Parsing is the rebuild. Linking, which is the part that has to see the whole workspace, costs almost nothing.

## Considered Options

* Keep re-parsing everything
* Cache the imported items per file and re-import only what changed
* Update the linked graph incrementally as well

## Decision Outcome

Chosen option: **cache the imported items per file, re-import only changed files, link everything**. The expensive phase becomes proportional to what the user touched, while the phase that needs global knowledge stays a full pass and therefore stays correct by construction.

A file counts as unchanged when its size and its modification time match the ones it had at the last import. Both come from the directory walk, which reads them anyway, so the check costs no extra file access. Hashing the content instead would be exact but was measured at 686 ms for those 6000 files, which is most of what the cache saves.

A file the editor holds open is never cached and always imported again. Its text is the buffer, not the file, and the file says nothing about it (`req~index-reads-open-documents~1`).

Updating the linked graph incrementally was rejected: a link status such as `ORPHANED` or `AMBIGUOUS` depends on items in other files, so a change would have to be propagated across the graph for 4 ms of savings.

### Consequences

* Good, because a rebuild after one edit no longer scales with the size of the workspace. Measured on 6000 files: 1253 ms for a full build, 73 ms for a rebuild.
* Good, because changes made outside the editor are picked up like any other, by their timestamp. A branch switch invalidates exactly the files it rewrote.
* Good, because the cache cannot drift into a different result: the items of an unchanged file are the ones it produced, and linking sees all of them.
* Bad, because an edit that restores both size and modification time is invisible to the rebuild. That takes deliberate effort, or a filesystem whose timestamps are coarser than the interval between two writes of equal length.

### Confirmation

Integration tests for different cases.
