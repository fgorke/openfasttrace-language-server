# OpenFastTrace Language Server 0.4, released 2026-08-09

Version 0.4 shows coverage where the specification is written. The chain of a specification item opens as a type hierarchy, from the feature it belongs to down to the coverage tags in the source and a summary above each item names the artifact types it still needs and those already covering it. Both clients now also run the server without a separate Java installation and OpenFastTrace is updated to 4.9.0.

## Features

* #14: both clients run the language server without a Java installation on the machine
* #15: coverage chain of a specification item shown as a type hierarchy, down to the coverage tags in the source
* #16: coverage summary above each specification item, naming the artifact types it still needs and those already covering it

## Bug Fixes

* #18: type hierarchy no longer hangs in VS Code, where deserializing the client's request failed

## Maintenance

* #17: updated OpenFastTrace from 4.8.0 to 4.9.0
* #17: only files an OpenFastTrace importer can read are passed to the import, which roughly halves indexing time
