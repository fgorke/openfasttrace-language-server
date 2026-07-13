# OpenFastTrace Language Server

Integrates the [OpenFastTrace](https://github.com/itsallcode/openfasttrace) (OFT) language server into VS Code via the built-in Language Server Protocol client. The server understands OFT specification items and coverage tags and provides:

* **Hover documentation**: title and description of the referenced specification item
* **Go to Definition**: from a coverage tag to the specification item and from an item to all its covering tags
* **Find References**: all coverage tags in the workspace that cover a specification item
* **Diagnostics and quick fix**: warnings for outdated coverage tag revisions with a one-click version update
* **Syntax highlighting**: specification item definitions, section keywords and coverage tags via semantic tokens
* **Code completion**: specification item IDs in `Covers:` sections and coverage tag targets
* **Tag skeletons**: invoking completion manually in a comment offers `[impl->...]`, `[utest->...]`, `[itest->...]` and `[stest->...]`

The server starts automatically when a supported file is opened. No configuration is required.

## Requirements

* Java 17 or later on `PATH` (or configured via `oft.java.path`)

## Settings

* `oft.java.path`: path to the `java` executable used to run the language server. Defaults to `java` on `PATH`.
