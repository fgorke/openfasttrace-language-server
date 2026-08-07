# OpenFastTrace Language Server

Integrates the [OpenFastTrace](https://github.com/itsallcode/openfasttrace) (OFT) language server into VS Code via the built-in Language Server Protocol client. The server understands OFT specification items and coverage tags and provides:

* **Hover documentation**: title and description of the referenced specification item
* **Go to Definition**: from a coverage tag to the specification item and from an item to all its covering tags
* **Find References**: all coverage tags in the workspace that cover a specification item
* **Trace diagnostics**: uncovered specification items, links to items that do not exist and duplicate definitions, reported while editing
* **Quick fix**: one-click update of an outdated coverage tag revision
* **Rename**: renames a specification item together with every coverage tag and reference in the workspace (F2)
* **Symbol search**: finds specification items through *Go to Symbol in Workspace* (Ctrl+T)
* **Syntax highlighting**: specification item definitions, section keywords and coverage tags via semantic tokens
* **Code completion**: specification item IDs in `Covers:` sections and coverage tag targets
* **Tag skeletons**: invoking completion manually in a comment offers `[impl->...]`, `[utest->...]`, `[itest->...]` and `[stest->...]`

The server starts automatically when a supported file is opened. No configuration is required.

## Requirements

* Java 17 or later on `PATH` (or configured via `oft.java.path`)

## Settings

* `oft.java.path`: path to the `java` executable used to run the language server. Defaults to `java` on `PATH`.
