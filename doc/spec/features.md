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
