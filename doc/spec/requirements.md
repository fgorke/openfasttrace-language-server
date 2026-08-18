# Requirements

## Go to Definition: Coverage Tag to Spec Item
req~goto-definition-tag-to-spec~2

When the user invokes Go to Definition with the cursor on a coverage tag, the server returns the location of the referenced specification item. The same holds for every other reference to an ID, and it holds no matter which file the item is declared in. 

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
req~hover-title-and-description~2

When the user hovers over a coverage tag, the server returns a Markdown response with the specification item's title and description, together with the columns of the whole ID so that the editor marks all of it.

Covers:
* feat~hover-documentation~1

Needs: impl, utest

## Diagnostic for Outdated Version
req~diagnostic-outdated-version~2

The server emits a diagnostic warning for every coverage tag whose referenced revision does not match the current revision of the specification item. That holds for a revision behind the item as well as for one ahead of it, and both carry the current ID for the quick fix to work from.

Covers:
* feat~quickfix-outdated-version~1

Needs: impl, itest

## Quick Fix Updates Version Number
req~quickfix-updates-version~1

Each diagnostic from `req~diagnostic-outdated-version~2` comes with a code action. The action replaces the mismatched revision number in the coverage tag with the current revision of the specification item.

Covers:
* feat~quickfix-outdated-version~1

Needs: impl, utest

## Quick Fix Updates All Outdated References
req~quickfix-updates-all-versions~2

A code action lifts every outdated reference to a specification item to its current revision across the workspace. It is offered at both ends: on a coverage tag and on the spec item itself.

Covers:
* feat~quickfix-outdated-version~1

Needs: impl, utest, itest

## Workspace Indexing on Startup
req~index-on-startup~3

On the `initialized` notification, the server imports all OFT-traceable files in the workspace. It builds an internal index from them. Hidden paths and build output directories (`target`, `build`, `out`, `dist`, `node_modules`) are skipped at every depth, so copied specification files do not appear twice.

The index is built off the message loop and reported through `window/workDoneProgress`. Until it is ready, requests are answered from an empty index. Matching an item to its file works on the path alone, without reading the file system.

Needs: impl, utest, itest

## Ignore File Excludes Paths From OFT Features
req~index-ignore-file~1

A file named `.oftignore` in the workspace root lists glob patterns, one per line. Blank lines and lines starting with `#` are skipped. A pattern matching a directory excludes everything below it. Matching files are not imported into the index and get neither semantic tokens nor completion.

Needs: impl, utest, itest

## Index Refresh on File Save
req~index-refresh-on-save~2

On `textDocument/didSave` or `workspace/didChangeWatchedFiles`, the server refreshes the index within 300 ms.

Where the editor supports it, the server therefore registers a watcher for `**/.oftignore` and learns of the change that way. Where it does not, the file takes effect on the next save of another file.

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
req~highlight-coverage-tag~2

When a coverage tag (e.g. `[impl->req~login~1]`) appears in a line, the server reports its full span as a semantic token of type `macro`. The span includes the brackets and the arrow. All optional parts OpenFastTrace accepts belong to the tag: an own revision (`[impl~~2->...]`), an own name and revision (`[impl~login~2->...]`) and needed coverage (`[dsn->req~login~1>>impl,utest]`).

Covers:
* feat~syntax-highlighting~1

Needs: impl, utest

## Complete Specification Item ID in Covers Section
req~complete-specification-item-id-in-covers-section~2

When the cursor sits inside a `Covers:` section of a specification file, the server suggests specification item IDs that match the text already typed. It ranks them by match quality: full ID prefix first, then name prefix, then name substring, then artifact type prefix. An item cannot cover itself.

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
req~suggest-coverage-tag-start-in-comment~3

In a comment line without an open coverage tag, the server suggests a coverage tag skeleton, e.g. `[impl->...]`, for every artifact type the specification items of the workspace need. Both editors only ask for skeletons on Ctrl+Space, while an open tag keeps its automatic suggestions.

Covers:
* feat~code-completion~1

Needs: impl, utest

## Find Specification Items Through Workspace Symbol Search
req~workspace-symbol-search~1

On `workspace/symbol` the server returns every specification item whose ID or title contains the query, ignoring case. An empty query returns all of them, because editors send one to fill their initial list. Results are ordered by name. Coverage tags are excluded: several tags typically point at the same item, so including them would answer a search for one item with a list of near-identical entries. `req~find-references-covering-tags~1` is the request that reports them.

Covers:
* feat~symbol-search~1

Needs: impl, utest

## Symbol Naming
req~symbol-naming~1

A specification item is reported with its ID as the symbol name, because that is what users type when searching

Covers:
* feat~symbol-search~1

Needs: impl, utest

## Rename Updates Definition and All References
req~rename-specification-item~1

On `textDocument/rename` with the cursor on a specification item ID, the server returns a workspace edit replacing that ID's name everywhere it occurs: in the item's own definition, in every coverage tag and in every `Covers:` or `Depends:` entry.

Covers:
* feat~rename~1

Needs: impl, utest, itest

## Rename Changes Only the Name Part
req~rename-name-part-only~1

Only the name part of an ID changes, artifact type and revision stay untouched. The new name has to match the OFT item name pattern, otherwise the server rejects the request with an error the editor shows.

Covers:
* feat~rename~1

Needs: impl, utest

## Prepare Rename Reports the Name Range
req~prepare-rename~1

On `textDocument/prepareRename` the server returns the range of the name part and the current name as placeholder, so the editor offers exactly that for editing.

Covers:
* feat~rename~1

Needs: impl, utest

## Rename Rejects a Conflicting Target Name
req~rename-conflict-check~1

Before computing edits, the server checks whether an item of the same artifact type already exists under the requested name. If it does, the server rejects the rename with an error the editor shows, rather than silently creating two declarations with the same ID.

Covers:
* feat~rename~1

Needs: impl, utest

## Diagnostic for Trace Defects
req~diagnostic-trace-defects~3

The server links the indexed specification items and reports every resulting defect as a diagnostic at the location of the item that caused it. Covered are links to items that do not exist, links to an ambiguous or unwanted target, specification items missing a required artifact type, duplicate definitions and coverage cycles. After every index rebuild the diagnostics are published for each affected file in the workspace.

The severity says what it takes to get rid of the defect. An **error** means text that is already written is wrong, a **warning** means the trace is merely unfinished, an **information** only reports that something further down the chain is incomplete.

Covers:
* feat~trace-in-editor~1

Needs: impl, itest

## Coverage Hierarchy
req~coverage-hierarchy~2

The hierarchy is opened on a specification item ID. The server reports the top of the coverage chain, so it is complete no matter where in the chain that item sits. From there, the items covering an item are its subtypes and the items it covers are its supertypes. Coverage tags appear as the innermost subtypes.

Deserializing a `TypeHierarchyItem` sent by the client needs a Gson instance creator, because LSP4J declares no parameterless constructor for it.

Covers:
* feat~coverage-hierarchy~1

Needs: impl, itest

## Coverage Code Lens
req~coverage-code-lens~1

The server reports a code lens above each specification item, naming the artifact types it still needs and the artifact types already covering it. An item that neither needs nor has coverage gets no lens, and coverage tags get none either.

Covers:
* feat~coverage-code-lens~1

Needs: impl, itest

## Precise Ranges from OFT
req~precise-ranges-from-oft~1

Where OpenFastTrace reports the source range of a specification item ID, the server uses that range instead of searching the line for the ID. If an importer supplies no range, the server falls back to scanning the line.

Covers:
* feat~trace-in-editor~1
* feat~goto-definition~1

Needs: impl, itest
## Supported Files from OFT
req~supported-files-from-oft~1

Whether OpenFastTrace can read a file is decided by asking the importers on the classpath, not by a list of file extensions kept in the server. The workspace walk passes only those files to OFT, so files no importer reads never enter the import.

Covers:
* feat~trace-in-editor~1

Needs: impl, itest

## Trace Report on Request
req~trace-report-on-request~1

On the `oft.generateTraceReport` command the server renders a trace report with OpenFastTrace's own reporters, writes it to a temporary file and answers with its path, which the client then opens. The command takes a preset naming the output format and the level of detail.

Covers:
* feat~trace-report-on-request~1

Needs: impl, utest, itest
