# OpenFastTrace Language Server

Integrates the [OpenFastTrace](https://github.com/itsallcode/openfasttrace) (OFT) language server into VS Code via the built-in Language Server Protocol client. The server understands OFT specification items and coverage tags and provides:

* **Hover documentation**: shows what the referenced specification item says
* **Go to definition**: from a coverage tag to the specification item and from an item to all its covering tags
* **Find references**: all coverage tags in the workspace that cover a specification item
* **Trace diagnostics**: uncovered specification items, links to items that do not exist and duplicate definitions, reported for every file in the Problems view
* **Quick fix**: one-click update of an outdated coverage tag revision, for a single tag or for every outdated reference to the item at once
* **Rename**: renames a specification item together with every coverage tag and reference in the workspace (F2)
* **Trace report**: renders a full OpenFastTrace report through the *OpenFastTrace: Generate Trace Report* command and opens it in the editor
* **Coverage code lens**: a summary above each specification item naming the artifact types it still needs and the artifact types already covering it
* **Coverage hierarchy**: the full coverage chain as a type hierarchy (*Show Type Hierarchy*), always starting at the top and reaching down to the coverage tags in source. Opened on a specification item ID
* **Symbol search**: finds specification items through *Go to Symbol in Workspace* (Ctrl+T)
* **Syntax highlighting**: specification item definitions, section keywords and coverage tags via semantic tokens
* **Code completion**: specification item IDs in `Covers:` sections and coverage tag targets
* **Tag skeletons**: invoking completion manually in a comment offers a skeleton such as `[impl->...]` for every artifact type the workspace needs, including project specific ones
* **Trace decisions**: an architecture decision record written as [MADR](https://github.com/adr/madr) can carry a specification item ID and is then traced like a requirement
* **Ignore file**: glob patterns in a `.oftignore` file in the workspace root exclude files from all OFT features, on top of the build output and hidden paths that are excluded by default

The server starts automatically when a supported file is opened. No configuration is required.

## Requirements

Every released package bundles a Java runtime, so nothing else needs to be installed. Packages exist for `win32-x64`, `linux-x64`, `linux-arm64`, `darwin-x64` and `darwin-arm64`.

On a platform outside that list, the extension falls back to Java 17 or later on the `PATH`. Setting `oft.java.path` always takes precedence over the bundled runtime.

## Settings

* `oft.java.path`: path to the `java` executable used to run the language server. Defaults to `java` on `PATH`.
