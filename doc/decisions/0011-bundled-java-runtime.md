---
status: accepted
date: 2026-08-07
decision-makers: Felix Gorke
---

# Bundled Java Runtimes Instead of a Native Image

## Context and Problem Statement

Both clients started the server with `java` from the `PATH`. That fails without a JDK, and equally with a Java older than 17.

## Considered Options

* Keep requiring Java on the `PATH`
* Run the server on the runtime the IDE already has
* Bundle a jlink runtime with the client
* Compile the server to a native image with GraalVM
* Download a runtime on first start

## Decision Outcome

Chosen option: **the IntelliJ plugin uses the runtime the IDE runs on, the VS Code extension bundles a jlink runtime per platform**.

IntelliJ ships a JetBrains Runtime, so `System.getProperty("java.home")` is enough there. VS Code has no JVM, so packages are built for `win32-x64`, `linux-x64`, `linux-arm64`, `darwin-x64` and `darwin-arm64`.

GraalVM was rejected. LSP4J ships [no native-image metadata](https://github.com/eclipse-lsp4j/lsp4j/issues/349), therefore the server must handle reflection itself.

Downloading on first start keeps the package small, but adds a download and a cache for a file and also requires a host.

### Consequences

* Good, because neither client needs a JDK on the machine.
* Bad, because the VS Code package grows from 1.7 MB to 24 MB, and five packages are released instead of one.
* Bad, because each platform needs a matching CI runner. jlink only builds for the platform it runs on.
* Neutral, because a platform without a released package still falls back to `java` on the `PATH`.

### Confirmation

The extension prefers a configured `oft.java.path`, then the bundled runtime, then the `PATH`.
