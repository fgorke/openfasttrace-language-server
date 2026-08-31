# Changelog

## 0.8.0

* Architecture decision records written as [MADR](https://github.com/adr/madr) can now be traced like requirements
* The workspace index is refreshed when files change outside the editor

## 0.7.0

* Every outdated reference to a specification item can be updated from the item itself
* Go to definition on a `Covers:` entry leads to the item it names
* An item is left out of the suggestions for its own `Covers:` section
* The extension is available from the Open VSX Registry

## 0.6.0

* Trace defects carry a severity: a reference that cannot be resolved, an ID defined twice and a coverage cycle are errors, coverage that is still missing is a warning
* Changes to a `.oftignore` file are picked up as soon as it is saved
* Coverage tag skeletons are offered in code comments, while `#` and `--` count as markup in Markdown and reStructuredText
* Hovering a specification item ID highlights the whole ID

## 0.5.0

* Trace defects are reported for every file in the workspace, not only for the open ones
* A quick fix updates every outdated reference to a specification item at once
* Glob patterns in a `.oftignore` file exclude paths from all OFT features
* Coverage tag skeletons cover the artifact types the workspace needs, and tags with an own revision or with needed coverage are recognized
* Indexing a large workspace runs in the background with a progress indicator and is faster

## 0.4.0

* Released once per platform with a Java runtime bundled, so no separate Java installation is needed
* The coverage chain of a specification item is shown as a type hierarchy, down to the coverage tags in the source
* A coverage summary above each specification item names the artifact types it still needs and those already covering it
* *OpenFastTrace: Generate Trace Report* creates a full report as HTML or plain text and opens it
* Updated OpenFastTrace to 4.9.0

## 0.3.0

* Trace defects are reported as diagnostics while editing: uncovered specification items, links to items that do not exist and duplicate definitions
* Rename a specification item together with every coverage tag and reference in the workspace
* Find specification items through *Go to Symbol in Workspace*
* The language server starts when a project is opened, instead of waiting for a file to be opened
* Coverage tags covering several items, such as `[impl->req~a~1, req~b~1]`, are highlighted and completed
* Updated OpenFastTrace to 4.8.0

## 0.2.0

* First release of the VS Code extension
