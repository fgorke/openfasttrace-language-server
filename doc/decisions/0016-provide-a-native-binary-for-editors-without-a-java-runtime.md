---
status: accepted
date: 2026-10-10
decision-makers: Oliver Kopp
---

# Provide a Native Binary for Editors Without a Java Runtime
`adr~provide-a-native-binary-for-editors-without-a-java-runtime~1`

Needs: impl

## Context and Problem Statement

[ADR 0011](0011-bundle-java-runtimes-instead-of-a-native-image.md) freed the two IDE clients from a Java installation. Editors such as Vim with [vim-lsp](https://github.com/prabirshrestha/vim-lsp), Neovim, Emacs or Helix have neither a JVM nor an extension package that could carry one. Their users had to install Java and launch the standalone JAR by hand, and with [ADR 0015](0015-use-java-25-as-minimum-runtime.md) that Java now has to be 25.

ADR 0011 rejected GraalVM because LSP4J ships no Native Image metadata, so every protocol type Gson touches through reflection would be missing from the image.

## Considered Options

* Keep requiring Java on the `PATH` for these editors
* Publish a jlink runtime together with the JAR per platform
* Compile the server to a native binary with GraalVM Native Image

## Decision Outcome

Chosen option: **a native binary per platform, built with GraalVM Native Image**.

The reflection problem is solved in the server itself: a GraalVM `Feature` ([`NativeImageSupport`](../../src/main/java/org/itsallcode/openfasttrace/lsp/nativeimage/NativeImageSupport.java)) walks the classpath while the image is built and registers every class of LSP4J, tinylog and the server for reflection, and registers the dynamic proxy LSP4J uses for the client. That stays correct when LSP4J adds types, unlike a reflection configuration recorded from one session. Resources OpenFastTrace reads and the build options live next to it in `META-INF/native-image/`, so `native-image -jar` on the standalone JAR works as well.

A jlink bundle would have been five archives of 24 MB each that still need an unpacking step and a launcher script per platform.

### Consequences

* Good, because the editor starts one file that needs nothing installed and is ready in milliseconds.
* Good, because the binary runs on any x86-64 or AArch64 machine, not only on CPUs as new as the build machine (`-march=compatibility`).
* Bad, because the release carries five more artifacts and each takes minutes to build.
* Bad, because a reflection gap shows only at run time. The smoke test in CI exists for that.
* Neutral, because the IntelliJ plugin and the VS Code extension keep the approach of ADR 0011. The VS Code extension could switch to the binary later and shrink from 24 MB to the size of the binary.

### Confirmation

CI builds the binary on all five platforms and runs [`native_smoke_test.sh`](../../.github/workflows/native_smoke_test.sh) against it: one LSP session over stdio against the demo project that checks the `initialize` answer, the published trace diagnostics, a `workspace/symbol` answer and a clean exit.

## More Information

The binaries are released as `openfasttrace-language-server-<version>-<platform>`, with the same platform names as the VS Code packages.

GraalVM Community for macOS on Intel ended with JDK 25.0.1, so `darwin-x64` is built with that release while the other platforms take the latest 25.x. The build therefore avoids anything newer releases introduced: the GraalVM metadata repository is switched off in the `native` profile, and the metadata the server needs is registered by `NativeImageSupport` or listed in `META-INF/native-image/`.
