---
status: accepted
date: 2026-08-25
decision-makers: Felix Gorke
---

# Trace Architecture Decision Records as Specification Items
`adr~trace-architecture-decision-records-as-specification-items~1`

Needs: impl

## Context and Problem Statement

Architecture decisions are written down and then forgotten. Nothing connects a decision to the code that carries it out, so nobody notices when the two drift apart. [e-ADR](https://github.com/adr/e-adr) closes that gap with Java annotations, `@ADR(1)` on a class or method, but only in Java, only where an annotation fits, and without checking that ADR 1 exists.

A specification item and an architecture decision are the same shape: something written in Markdown that code is supposed to fulfil. OpenFastTrace traces the first already.

## Considered Options

* Give an ADR an OFT ID and let the existing infrastructure do the rest
* A separate ADR mode in the server, with its own model and syntax
* Read e-ADR annotations from Java source and turn them into trace links

## Decision Outcome

Chosen option: **an ADR carries an OFT ID like any other specification item.** An ID line under the title turns a [MADR](https://adr.github.io/madr/) file into one:

```markdown
# Cache the Workspace Index on Disk
`adr~cache-the-workspace-index-on-disk~1`

Needs: impl
```

From there `// [impl->adr~cache-the-workspace-index-on-disk~1]` links code to the decision, and every feature of this server applies without a line of new code. Only the convenience is missing, so that is the whole implementation: a code action on the title inserts the ID.

**The name comes from the file name, without the number.** MADR names a file after its title, so the file name is the title in a form an ID can carry. OFT requires an item name to start with a letter, `\p{Alpha}[\w-]*`, so the number stays where it already is.

### Consequences

* Good, because the whole feature set applies to decisions at once, and the only new code is one code action.
* Good, because it works in every language OFT reads tags in, not only Java, and at any line rather than only where an annotation fits.
* Good, because a reference to a decision that does not exist is reported, which `@ADR(1)` cannot do.
* Bad, because MADR knows `superseded` and OFT does not. A superseded decision keeps demanding coverage until its ID is removed by hand.
* Neutral, because e-ADR annotations are left alone. Reading them would mean parsing Java, which belongs in an OFT importer.

### Confirmation

Unit tests cover `req~generate-specification-item-id-for-adr~1`. The example is this repository, whose decisions live in `doc/decisions/`.
