# OpenFastTrace Language Server 0.8, released 2026-09-01

Version 0.8 extends tracing to architecture decision records and fixes two
problems that made the plugin unusable for some IntelliJ users.

The IntelliJ plugin now requires **2026.1.4** or later.

## Features

* #58: architecture decision records written as MADR traced as specification items, with a code action that inserts the ID

## Bug Fixes

* #59: the plugin no longer installs into IntelliJ builds it cannot run
* #61: the index is refreshed when files change outside the editor, for example on a checkout or a stash

## Documentation

* #54: OFT user guide references updated
* #55: feature guide and README reworked

## Maintenance

* #56: findings from static analysis resolved
* #57: every platform package published to the Open VSX Registry
