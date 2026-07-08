---
status: accepted
date: 2026-05-13
decision-makers: Felix Gorke
---

# Build Tool: Maven

## Context and Problem Statement

The project needs a JVM build tool. It has to handle dependencies, compilation and testing. It also has to package a runnable fat JAR and run in CI.

## Considered Options

* Maven
* Gradle (Kotlin DSL)

## Decision Outcome

Chosen option: **Maven**. OpenFastTrace itself is built with Maven. That keeps the dependency setup consistent. It also makes a future contribution back to the itsallcode organisation easier.

### Consequences

* Good, because OFT's own dependency resolution carries over without surprises.
* Good, because the standard layout (`src/main/java`, `src/test/java`) needs no explanation.
* Good, because shade, JaCoCo and Surefire are mature plugins with plenty of documentation.
* Bad, because the XML is more verbose than Gradle's Kotlin DSL.
* Bad, because incremental builds are slower than Gradle's.
