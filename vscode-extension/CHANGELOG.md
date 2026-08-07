# Changelog

## 0.3.0

* Trace defects are reported as diagnostics while editing: uncovered specification items, links to items that do not exist and duplicate definitions
* Rename a specification item together with every coverage tag and reference in the workspace
* Find specification items through *Go to Symbol in Workspace*
* The language server starts when a project is opened, instead of waiting for a file to be opened
* Coverage tags covering several items, such as `[impl->req~a~1, req~b~1]`, are highlighted and completed
* Updated OpenFastTrace to 4.8.0

## 0.2.0

* First release of the VS Code extension
