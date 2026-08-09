# Changelog

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
