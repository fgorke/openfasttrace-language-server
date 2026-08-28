---
status: accepted
date: 2026-05-13
decision-makers: Felix Gorke
---

# Use Java 17 as Minimum Runtime

## Context and Problem Statement

OpenFastTrace 4.x requires Java 17 as its minimum runtime. The language server has to run on the same JVM.

## Considered Options

* Java 17 (LTS)
* Java 21 (LTS)

## Decision Outcome

Chosen option: **Java 17**, configured via `maven.compiler.release=17`. Targeting 21 would exclude users still on 17 for no real gain. OFT itself does not go beyond 17 either.

### Consequences

* Good, because the language server and OFT share the same minimum JVM.
* Good, because 17 is LTS with solid IDE and CI support.
* Good, because records, sealed classes and pattern matching are already available.
* Neutral, because LSP4J 1.0.0 also supports 17+. This was not a constraint either way.

