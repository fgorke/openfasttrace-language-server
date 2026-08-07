# OpenFastTrace Language Server 0.3, released 2026-08-07

Version 0.3 brings the OpenFastTrace linker into the editor. Uncovered requirements, links pointing at items that do not exist and duplicate definitions now show up as diagnostics while editing, instead of only in a separate trace run. The release also adds workspace symbol search and rename, and updates OpenFastTrace to 4.8.0.

## Features

* #12: trace diagnostics in the editor, reporting uncovered requirements, links to non-existing items and duplicate definitions
* #10: added rename support for specification items, updating the definition and all references in the workspace
* #9: symbol search for specification items across the workspace, without knowing which file they live in
* #8: language server now starts automatically when a project is opened

## Bug Fixes

* #7: always selected the newest server build instead of the first one found

## Maintenance

* #11: updated OpenFastTrace from 4.5.0 to 4.8.0
