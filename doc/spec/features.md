# Features

## Go to Definition
feat~goto-definition~1

The language server links coverage tags and specification items in both directions. From a coverage tag in source code it jumps to the specification item in Markdown. From a specification item it jumps to all its covering tags.

Needs: req

## Find References
feat~find-references~1

The language server lists all coverage tags in the workspace that cover a given specification item.

Needs: req

## Hover Documentation
feat~hover-documentation~1

The language server shows a hover tooltip over a coverage tag. The tooltip contains the specification item's title and description.

Needs: req

## Quick Fix: Outdated Version
feat~quickfix-outdated-version~1

The language server detects coverage tags that reference an outdated revision of a specification item. It offers a quick fix to update the version number to the current revision.

Needs: req

## Syntax Highlighting
feat~syntax-highlighting~1

The language server highlights OpenFastTrace syntax directly in the editor. That covers specification item definitions, section keywords and coverage tags.

Needs: req

## Code Completion for Specification Item IDs
feat~code-completion~1

The language server suggests existing specification item IDs while the user types. It works in a `Covers:` entry or a coverage tag target. IDs then do not have to be typed out or looked up by hand.

Needs: req

## Symbol Search
feat~symbol-search~1

The language server reports specification items as symbols. The user finds them through the editor's symbol search across the whole workspace, without knowing which file they live in.

Needs: req

## Rename Specification Items
feat~rename~1

The user renames a specification item from anywhere it appears and the server updates the definition together with every coverage tag and reference in the workspace.

Needs: req

## Trace Results in the Editor
feat~trace-in-editor~1

The language server runs the OpenFastTrace linker over the workspace and reports every defect it finds as a diagnostic at the source position that caused it.

Needs: req

## Coverage Hierarchy
feat~coverage-hierarchy~1

The editor shows the full coverage chain as a hierarchy, starting at the top. It can be opened by invoking it on a specification item.

Needs: req

## Coverage Code Lens
feat~coverage-code-lens~1

The editor shows a short coverage summary above each specification item, so its state is visible while reading the specification.

Needs: req

## Trace Report on Request
feat~trace-report-on-request~1

The user can generate a trace report from the editor. The report is rendered by OpenFastTrace itself, as HTML or as plain text in several levels of detail.

Needs: req

## Trace Architecture Decisions
feat~trace-architecture-decisions~1

Architecture decision records are traced like any other specification, so the editor shows which decisions the code carries out and which are still only written down.

Needs: req
