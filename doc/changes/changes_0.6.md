# OpenFastTrace Language Server 0.6, released 2026-08-17

Version 0.6 is mostly bug fixes. Trace defects now carry a severity instead of all being warnings, a `.oftignore` file takes effect as soon as it is saved, and completion stays out of ordinary text.

## Features

* #46: changes to `.oftignore` picked up without saving another file

## Bug Fixes

* #45: broken references, duplicate definitions and coverage cycles reported as errors, while missing coverage stays a warning
* #43: coverage tag skeletons no longer offered in Markdown and reStructuredText, where `#` and `--` are markup rather than comments
* #47: hover marks the whole specification item ID instead of the part between its separators

## Maintenance

* #44: builds of the IntelliJ plugin available from every CI run, not only from a release
