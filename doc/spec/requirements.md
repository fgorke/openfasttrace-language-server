# Requirements

## Go to Definition: Coverage Tag to Spec Item
req~goto-definition-tag-to-spec~1

When the user invokes Go to Definition with the cursor on a coverage tag, the server returns the location of the referenced specification item.

Covers:
* feat~goto-definition~1

Needs: impl, utest

## Go to Definition: Spec Item to Covering Tags
req~goto-definition-spec-to-tags~1

When the user invokes Go to Definition with the cursor on a specification item ID, the server returns the locations of all coverage tags that cover that item.

Covers:
* feat~goto-definition~1

Needs: impl, utest

## Find References Returns All Covering Tags
req~find-references-covering-tags~1

When the user invokes Find References on a specification item, the server returns the file locations of all coverage tags in the workspace that reference that item's ID.

Covers:
* feat~find-references~1

Needs: impl, utest

## Hover Shows Title and Description
req~hover-title-and-description~1

When the user hovers over a coverage tag, the server returns a Markdown response with the specification item's title and description.

Covers:
* feat~hover-documentation~1

Needs: impl, utest

## Diagnostic for Outdated Version
req~diagnostic-outdated-version~1

The server emits a diagnostic warning for every coverage tag whose referenced revision does not match the current revision of the specification item.

Covers:
* feat~quickfix-outdated-version~1

Needs: impl, utest

## Quick Fix Updates Version Number
req~quickfix-updates-version~1

Each diagnostic from `req~diagnostic-outdated-version~1` comes with a code action. The action replaces the outdated revision number in the coverage tag with the current revision of the specification item.

Covers:
* feat~quickfix-outdated-version~1

Needs: impl, utest

## Workspace Indexing on Startup
req~index-on-startup~1

On the `initialized` notification, the server imports all OFT-traceable files in the workspace. It builds an internal index from them. Hidden directories and build output directories (`target`, `build`, `out`, `dist`, `node_modules`) are skipped, so copied specification files do not appear twice.

Needs: impl, utest, itest

## Index Refresh on File Save
req~index-refresh-on-save~1

On `textDocument/didSave` or `workspace/didChangeWatchedFiles`, the server refreshes the index within 300 ms.

Needs: impl, utest

## Live Document Buffer
req~live-document-buffer~1

The server keeps the current in-memory text of every open document. It updates that text on `textDocument/didOpen` and `textDocument/didChange`. Requests that work on document text read from this buffer while the document is open. Those requests are hover, go to definition, find references, semantic tokens and completion. When the document is not open, they fall back to the file on disk. This buffer is separate from the OFT workspace index, which is only rebuilt on save (`req~index-refresh-on-save~1`).

Needs: impl, utest, itest

## Highlight Specification Item Definitions
req~highlight-specification-item~1

When a line consists solely of a specification item ID (e.g. `req~login~1`), the server reports it as a semantic token of type `type`.

Covers:
* feat~syntax-highlighting~1

Needs: impl, utest

## Highlight Section Keywords
req~highlight-keyword~1

When a line starts with an OFT section keyword (`Needs`, `Covers`, `Depends`, `Status`, `Description`, `Rationale`, `Comment`, `Tags`) followed by a colon, the server reports the keyword as a semantic token of type `keyword`.

Covers:
* feat~syntax-highlighting~1

Needs: impl, utest

## Highlight Coverage Tags
req~highlight-coverage-tag~1

When a coverage tag (e.g. `[impl->req~login~1]`) appears in a line, the server reports its full span as a semantic token of type `macro`. The span includes the brackets and the arrow.

Covers:
* feat~syntax-highlighting~1

Needs: impl, utest

## Complete Specification Item ID in Covers Section
req~complete-specification-item-id-in-covers-section~1

When the cursor sits inside a `Covers:` section of a specification file, the server suggests specification item IDs that match the text already typed. It ranks them by match quality: full ID prefix first, then name prefix, then name substring, then artifact type prefix.

Covers:
* feat~code-completion~1

Needs: impl, utest

## Complete Specification Item ID in Coverage Tag Target
req~complete-specification-item-id-in-coverage-tag-target~1

When the cursor sits in the target half of an open coverage tag (e.g. `[impl->req~lo`) after a comment marker, the server suggests matching specification item IDs whose `Needs` list contains the tag's source artifact type.

Covers:
* feat~code-completion~1

Needs: impl, utest

## Complete Closing Bracket for Coverage Tag
req~complete-closing-bracket-for-coverage-tag~1

When a specification item ID is inserted to complete an open coverage tag target, the server also appends the closing bracket `]`, unless one already follows the cursor.

Covers:
* feat~code-completion~1 

Needs: impl, utest

## Suggest Coverage Tag Start in Comment
req~suggest-coverage-tag-start-in-comment~1

When completion is invoked manually in a comment line without an open coverage tag, the server suggests a coverage tag skeleton for each of `impl`, `utest`, `itest` and `stest`, e.g. `[impl->...]`.

Covers:
* feat~code-completion~1

Needs: impl, utest
