---
status: accepted
date: 2026-10-10
decision-makers: Oliver Kopp
---

# Ship the Native Binary in the VS Code Extension
`adr~ship-the-native-binary-in-the-vs-code-extension~1`

Needs: impl

## Context and Problem Statement

[ADR 0011](0011-bundle-java-runtimes-instead-of-a-native-image.md) gave the VS Code extension a jlink runtime per platform, which grew the package from 1.7 MB to 24 MB and tied every package to a CI runner that can run jlink for it. [ADR 0016](0016-provide-a-native-binary-for-editors-without-a-java-runtime.md) then produced a native binary of the server for every platform the extension is released for. Carrying both a Java runtime and a binary that needs none makes no sense, so which one does the extension ship?

## Considered Options

* Keep the jlink runtime and the JAR
* Ship the native binary, keep the JAR as fallback
* Ship the native binary only
* Download the binary on first start, as the [JabRef extension](https://github.com/JabRef/lsp-vscode-extension) does with its server

## Decision Outcome

Chosen option: **ship the native binary, keep the JAR as fallback**.

The platform-specific package carries `server/openfasttrace-language-server` (`.exe` on Windows) with a marker file naming its platform, and the extension starts it directly. The 1.8 MB JAR stays in the package for two cases: a configured `oft.java.path`, which now means "run the JAR with this Java instead of the binary", and a package built without a binary, which runs the JAR with `java` from the `PATH` as before.

Downloading on first start was rejected again for the reasons of ADR 0011: it needs a host and a cache, and the first start depends on the network.

### Consequences

* Good, because the package shrinks from 24 MB to 13 MB and starts the server in milliseconds.
* Good, because a package for any platform can be built on any machine once that platform's binary is at hand, which is how CI does it from the native build artifacts. jlink could only build for the machine it ran on.
* Good, because the VS Code extension and the editors of ADR 0016 run the same binary, so the CI smoke test covers both.
* Bad, because the VS Code package now waits for the native build in CI.
* Neutral, because the IntelliJ plugin keeps running the JAR on the IDE's runtime (ADR 0011), as the IDE always brings one.

### Confirmation

`scripts/package.js` fails when no binary for the target exists. CI downloads the binary that the native job built and smoke tested, and packages it for each of the five platforms.
