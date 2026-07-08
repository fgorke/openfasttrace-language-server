# OpenFastTrace Language Server

## What is OpenFastTrace Language Server?

[OpenFastTrace](https://github.com/itsallcode/openfasttrace) (short OFT) is a requirement tracing suite. It keeps track of whether you actually implemented everything you planned to in your specifications.

The OpenFastTrace Language Server brings OFT support into code editors through the [Language Server Protocol](https://microsoft.github.io/language-server-protocol/) (LSP). One server process understands OFT specification items and coverage tags. Any LSP-capable editor can connect to it. An IntelliJ IDEA plugin that does exactly that ships in this repository.

You can learn more about requirement tracing and the OFT format in the [OFT user guide](https://github.com/itsallcode/openfasttrace/blob/main/doc/user_guide.md).

## Project Information

* Bachelor thesis project, work in progress
* Java 17, [LSP4J](https://github.com/eclipse-lsp4j/lsp4j) 1.0.0 (LSP 3.18), OFT 4.3.0
* License: [GPL-3.0-or-later](LICENSE)
* Requirements: [doc/spec/](doc/spec/), architecture decisions: [doc/decisions/](doc/decisions/)

## Using the Language Server

The server provides these features to any connected editor:

* **Hover documentation.** Hovering over a coverage tag shows the referenced specification item's title and description.
* **Go to Definition.** A coverage tag jumps to the specification item in Markdown. A specification item ID jumps to all its covering tags.
* **Find References.** Lists every coverage tag in the workspace that covers a specification item.
* **Diagnostics with quick fix.** Coverage tags that reference an outdated revision get a warning. The quick fix updates the version number to the current revision.
* **Syntax highlighting.** Specification item definitions, section keywords and coverage tags are reported as semantic tokens.
* **Code completion.** Specification item IDs are suggested while typing a `Covers:` entry or a coverage tag target such as `[impl->`.
* **Tag skeletons.** Invoking completion manually in comments offers `[impl->...]`, `[utest->...]`, `[itest->...]` and `[stest->...]`.

## Getting the Project

```bash
git clone <this repository>
```

## Installation

### Runtime Dependencies

* Java 17 or later
* For the IntelliJ plugin: IntelliJ IDEA Ultimate (the LSP client API is not part of the Community edition)

### Build Dependencies

* Maven 3.6 or later for the server
* Gradle is bundled as a wrapper in `intellij-plugin/`

## Building

Build the server and run all tests:

```bash
mvn verify
```

The build produces a standalone fat JAR at `target/openfasttrace-language-server-*-standalone.jar`.

## Running

### Standalone

```bash
java -jar target/openfasttrace-language-server-*-standalone.jar
```

The server speaks LSP over stdio. Any editor with an LSP client can launch it this way.

### IntelliJ IDEA Plugin

The `intellij-plugin/` module wraps the server for IntelliJ IDEA Ultimate.

Run a sandbox IDE with the plugin during development:

```bash
mvn package
cd intellij-plugin
./gradlew runIde
```

Build an installable plugin ZIP:

```bash
mvn package
cd intellij-plugin
./gradlew buildPlugin
```

The ZIP lands in `intellij-plugin/build/distributions/`. Install it via **Settings, Plugins, gear icon, Install Plugin from Disk**. The Gradle build bundles the server JAR from `../target/` into the plugin, so always run `mvn package` first.

The plugin starts the server automatically when a supported file is opened. No manual setup is needed. `intellij-plugin/gradle.properties` controls the IntelliJ version used for the sandbox (`intellijVersion`) and the minimum supported build (`pluginSinceBuild`).

## Requirement Tracing

This project traces its own requirements with OpenFastTrace. Features live in [doc/spec/features.md](doc/spec/features.md), requirements in [doc/spec/requirements.md](doc/spec/requirements.md). The testing approach is documented in [ADR-0009](doc/decisions/0009-testing-strategy.md).

## Related Projects

* [OpenFastTrace](https://github.com/itsallcode/openfasttrace): the requirement tracing suite this server builds on
* [OpenFastTrace IntelliJ Plugin](https://github.com/itsallcode/openfasttrace-intellij-plugin): the native PSI-based IntelliJ integration by the itsallcode team
* [LSP4J](https://github.com/eclipse-lsp4j/lsp4j): the Java implementation of the Language Server Protocol used here

## Development

Tests follow a Given-When-Then structure and are split into unit and integration tests. See [ADR-0009](doc/decisions/0009-testing-strategy.md) for the full testing strategy.

```bash
mvn test
```

## License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

See [LICENSE](LICENSE) for the full text. OpenFastTrace is licensed under GPL-3.0. This project links OFT on the classpath, so it is GPL-3.0-or-later as well (see [ADR-0005](doc/decisions/0005-license.md)).
