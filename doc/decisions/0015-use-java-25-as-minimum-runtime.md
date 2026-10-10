---
status: accepted
date: 2026-10-10
decision-makers: Oliver Kopp
---

# Use Java 25 as Minimum Runtime
`adr~use-java-25-as-minimum-runtime~1`

Needs: impl

## Context and Problem Statement

[ADR 0002](0002-use-java-17-as-minimum-runtime.md) chose Java 17 so that anyone with a current JDK could run the server. Since [ADR 0011](0011-bundle-java-runtimes-instead-of-a-native-image.md) no client depends on a JDK on the machine: the IntelliJ plugin runs the server on the runtime of the IDE, the VS Code extension bundles one, and the native binary of [ADR 0016](0016-provide-a-native-binary-for-editors-without-a-java-runtime.md) needs none at all. The minimum runtime therefore constrains only the build and the IDE runtime, and Java 25 is the current LTS release.

## Decision Drivers

* The native binary is built with GraalVM for JDK 25, so the build runs on 25 anyway.
* IntelliJ 2026.1, the oldest IDE the plugin supports, runs on JetBrains Runtime 25.
* OpenFastTrace 4.9.0 is built for Java 17 and runs unchanged on 25.

## Considered Options

* Stay on Java 17
* Java 21 (LTS)
* Java 25 (LTS)

## Decision Outcome

Chosen option: **Java 25**, configured via `maven.compiler.release=25`. Nothing a user installs depends on the choice any more, so the server takes the current LTS and its language features instead of carrying the 17 restriction for nobody.

### Consequences

* Good, because the server, the native image and the bundled VS Code runtime are built from one JDK.
* Good, because the IntelliJ plugin is unaffected: every supported IDE build ships JetBrains Runtime 25.
* Bad, because the standalone JAR no longer runs on Java 17 to 24. Whoever launches the JAR directly needs Java 25 or uses the native binary.
* Neutral, because OpenFastTrace stays on 17 and keeps working on 25.

### Confirmation

CI builds and tests with JDK 25. JaCoCo 0.8.14 was the first release that instruments Java 25 class files, the build uses 0.8.15.
