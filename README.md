# OpenFastTrace Language Server

## What is OpenFastTrace Language Server?

[OpenFastTrace](https://github.com/itsallcode/openfasttrace) (short OFT) is a requirement tracing suite. It keeps track of whether you actually implemented everything you planned to in your specifications.

The OpenFastTrace Language Server brings OFT support into code editors through the [Language Server Protocol](https://microsoft.github.io/language-server-protocol/) (LSP). One server process understands OFT specification items and coverage tags. Any LSP-capable editor can connect to it. This repository ships two clients that do exactly that: an IntelliJ IDEA plugin and a VS Code extension.

You can learn more about requirement tracing and the OFT format in the [OFT user guide](https://github.com/itsallcode/openfasttrace/blob/main/doc/user_guide.md).

## Quickstart

The easiest way to get started with the OpenFastTrace Language Server is to install a pre-built client from the releases.

### IntelliJ IDEA

1. Download the latest plugin ZIP from the [Releases](../../releases) page.
2. Open IntelliJ IDEA Ultimate.
3. Go to **Settings → Plugins**.
4. Click the gear icon and select **Install Plugin from Disk**.
5. Select the downloaded ZIP file.
6. Restart IntelliJ IDEA when prompted.

### VS Code

1. Download the `.vsix` for your platform from the [Releases](../../releases) page. Each one carries its own Java runtime, so the file name ends in the platform, for example `-win32-x64` or `-darwin-arm64`.
2. Run `code --install-extension <downloaded-file>.vsix`, or open the Extensions view, click the `...` menu and choose **Install from VSIX...**.

Both clients start the OpenFastTrace Language Server automatically when a supported file is opened. No additional configuration is required.

For building a client yourself or running the language server standalone, see the development instructions below.

## Project Information

* Bachelor thesis project, work in progress
* Java 17, [LSP4J](https://github.com/eclipse-lsp4j/lsp4j) 1.0.0 (LSP 3.18), OFT 4.9.0
* License: [GPL-3.0-or-later](LICENSE)
* Requirements: [doc/spec/](doc/spec/), architecture decisions: [doc/decisions/](doc/decisions/)

## Using the Language Server

The server provides these features to any connected editor:

* **Hover documentation.** Hovering over a coverage tag shows the referenced specification item's title and description.
* **Go to Definition.** A coverage tag jumps to the specification item in Markdown. A specification item ID jumps to all its covering tags.
* **Find References.** Lists every coverage tag in the workspace that covers a specification item.
* **Trace diagnostics.** Specification items that are missing required coverage, coverage tags pointing at items that do not exist, and duplicate definitions are reported at the position that caused them.
* **Quick fix.** Coverage tags that reference an outdated revision get a warning with a quick fix that updates the version number to the current revision.
* **Syntax highlighting.** Specification item definitions, section keywords and coverage tags are reported as semantic tokens.
* **Code completion.** Specification item IDs are suggested while typing a `Covers:` entry or a coverage tag target such as `[impl->`.
* **Trace report.** A full OpenFastTrace report is rendered on request and opened in the editor, as HTML or plain text in several levels of detail.
* **Coverage code lens.** A short summary above each specification item names the artifact types it still needs and how many items cover it.
* **Coverage hierarchy.** The full coverage chain is shown as a type hierarchy, always starting at the top: downwards the items covering an item, ending at the coverage tags in the source code. It is opened on a specification item ID.
* **Symbol search.** Specification items are found through the editor's workspace symbol search.
* **Rename.** Renaming a specification item updates its definition together with every coverage tag and `Covers:` entry in the workspace.
* **Tag skeletons.** Invoking completion manually in comments offers a skeleton such as `[impl->...]` for every artifact type the workspace needs, including project specific ones.

## Getting the Project

```bash
git clone https://github.com/fgorke/openfasttrace-language-server.git
```

## Installation

### Runtime Dependencies

* For the IntelliJ plugin: IntelliJ IDEA Ultimate 2026.1 or later.
* For the VS Code extension: VS Code 1.82 or later.
* For the standalone server: Java 17 or later

### Build Dependencies

* Maven 3.6 or later for the server
* Gradle is bundled as a wrapper in `intellij-plugin/`
* Node.js and npm for the VS Code extension in `vscode-extension/`

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

### IntelliJ IDEA Plugin (Development)

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

The ZIP lands in `intellij-plugin/build/distributions/`.

### VS Code Extension (Development)

The `vscode-extension/` module wraps the server for VS Code.

Run an Extension Development Host during development:

```bash
mvn package
cd vscode-extension
npm install
npm run watch
```

Then press F5 in VS Code (with `vscode-extension/` open as the workspace) to launch it.

Build an installable package with a Java runtime bundled for the current machine:

```bash
mvn package
cd vscode-extension
npm install
npm run package
```

The `.vsix` lands in `vscode-extension/` and its name ends in the platform, for example `-win32-x64`.

`jlink` only builds a runtime for the platform it runs on.

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

See [LICENSE](LICENSE) for the full text. OpenFastTrace is licensed under GPL-3.0-or-later. This project links OFT on the classpath, so it is GPL-3.0-or-later as well (see [ADR-0005](doc/decisions/0005-license.md)).
