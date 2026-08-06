---
status: accepted
date: 2026-05-13
decision-makers: Felix Gorke
---

# OFT Dependency Strategy: Individual Modules, Pinned to 4.8.0

## Context and Problem Statement

OpenFastTrace ships on Maven Central two ways. There is an uber-JAR (`org.itsallcode:openfasttrace`). There are also separate modules (`-api`, `-core`, `-importer-markdown`, `-importer-tag`, …). Either way, the project should not reimplement OFT's parsing or linking.

## Considered Options

* Individual OFT modules (api, core, importer-markdown, importer-tag)
* OFT uber-JAR

## Decision Outcome

Chosen option: **individual modules, pinned to 4.8.0**. This keeps unneeded exporters and reporters off the classpath. It shrinks the fat JAR. It makes clear exactly what is used.

Updated from 4.3.0 to 4.5.0 on 2026-07-08, and from 4.5.0 to 4.8.0 on 2026-08-06.

Modules included:

| Artifact | Reason |
|---|---|
| `openfasttrace-api` | Core interfaces: `SpecificationItem`, `Location`, `ImportSettings` |
| `openfasttrace-core` | `Oft.create()`, linker, tracer |
| `openfasttrace-importer-markdown` | Parses spec items from `.md` files |
| `openfasttrace-importer-tag` | Parses coverage tags from source files |
| `openfasttrace-importer-gherkin` | Parses coverage tags from `.feature` files |
| `openfasttrace-importer-restructuredtext` | Parses spec items from `.rst` files |

**Update policy:** bumping the OFT version means updating this ADR, checking API compatibility, bumping `openfasttrace.version` and re-checking which importer modules exist.

### Consequences

* Good, because unused OFT modules (reporters, specobject exporter, zip importer) never end up on the classpath.
* Good, because the dependency list itself documents the OFT API surface in use.
* Bad, because each module has to be listed separately instead of one uber-JAR line.
* Bad, because OFT's internal module boundaries can shift between releases. That means revisiting this list.

The 4.8.0 bump is a concrete case of that last point. OFT 4.7.0 moved Gherkin out of `openfasttrace-importer-tag` into its own module, so `.feature` files silently stopped being indexed: importers are discovered through `ServiceLoader`, so a missing module produces neither a build nor a test failure.

## Pros and Cons of the Options

### Uber-JAR

* Good, because one dependency line covers everything.
* Bad, because it pulls in every exporter, reporter and importer regardless of need.
* Bad, because it is meant for CLI use, not as a library dependency.
