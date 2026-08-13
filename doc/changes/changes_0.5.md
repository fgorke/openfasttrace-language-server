# OpenFastTrace Language Server 0.5, released 2026-08-13

Version 0.5 reports the trace of the whole workspace instead of just opened files in the editor's problems view. A `.oftignore` file keeps documentation and generated content out of the trace and indexing was improved.

## Features

* #23: `.oftignore` file excludes paths from all OFT features, on top of build output and hidden paths that are excluded by default
* #24: trace defects reported for every file in the workspace, listed in the editor's problems view
* #26: quick fix that updates every outdated reference to a specification item across the workspace

## Bug Fixes

* #21: coverage tag skeletons offered for the artifact types the workspace needs, instead of a fixed list
* #22: coverage tags with an own revision and tags declaring needed coverage recognized
* #27: severity of a problem shown in its icon instead of an error mark on everything

## Maintenance

* #25: workspace indexing improved and reported as a running task
* #28: demo project with a walkthrough through every feature
