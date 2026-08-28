---
status: accepted
date: 2026-05-13
decision-makers: Felix Gorke
---

# Use a Dedicated Converter for URIs and Positions
`adr~use-a-dedicated-converter-for-uris-and-positions~1`

Needs: impl

## Context and Problem Statement

LSP represents locations as a `DocumentUri` (`file://` string). Positions are 0-based `{line, character}` pairs. OFT's `Location` uses a file path string, a 1-based line number and an optional column (`NO_LINE = -1`, `NO_COLUMN = -1`). Something has to bridge the two consistently. It needs to be tested.

## Considered Options

* Explicit conversion utilities in a dedicated `LocationConverter` class
* Ad-hoc conversion inline at each LSP handler call site

## Decision Outcome

Chosen option: **a dedicated `LocationConverter` class**, with these rules:

| Concern | Rule |
|---|---|
| URI → Path | `Path.of(URI.create(documentUri))` |
| Path → URI | `path.toUri().toString()` (with manual `file://` handling for already-URI or absolute Unix paths) |
| Path or URI → file key | `path.toAbsolutePath().normalize()`, lowercased on Windows |
| OFT line → LSP line | `Math.max(0, oftLine - 1)` (OFT is 1-based, LSP is 0-based) |
| Range start character | `0` by default; tightened to the element's start column when the target line text is available |
| Range end character | `Integer.MAX_VALUE` by default; tightened to the element's end column when the target line text is available |

The file key exists because the same file arrives as a URI from the client and as a path from OFT, and comparing the two needs one spelling. `toRealPath` would deliver the spelling the file system itself uses, symbolic links resolved, but it reads the disk, and the index builds a key per specification item.

OFT column information is not used by the converter. By default a location spans the full source line. For navigation results (go-to-definition and find-references) the converter also gets the target line text. It tightens the range to the coverage tag (e.g. `[impl->req~login~1]`). Failing that, it uses the first OFT ID on the line. This keeps the conversion free of OFT column tracking. It still gives IntelliJ a meaningful range. IntelliJ's "Choose Declaration" popup renders the text under the returned range. A full-line range showed the raw comment line (`// [impl->req...`). The tight range shows the tag itself. When the line cannot be read, it falls back to the full-line range.

### Consequences

* Good, because the conversion logic lives in one place. It is unit-tested on its own, separate from the LSP server.
* Good, because off-by-one errors in line and column conversion are easy to catch when isolated like this.
* Good, because navigation results carry a range tight to the coverage tag. IntelliJ's multi-target "Choose Declaration" popup then shows the tag (`[impl->req~login~1]`) instead of the raw comment line.
* Bad, because two paths that reach the same file through a symbolic link count as two files.
* Bad, because tightening reads the target line once per navigation result. That is fine for the small number of targets a navigation request returns. It falls back to the full-line range when the line cannot be read.

### Confirmation

Unit tests for `LocationConverter` cover the URI round-trip, including already-URI and absolute Unix path inputs. They cover line offset conversion from a 1-based OFT line to a 0-based LSP line, clamped at 0. They cover range tightening to the coverage tag or spec item ID when line text is supplied, with full-line fallback otherwise.
