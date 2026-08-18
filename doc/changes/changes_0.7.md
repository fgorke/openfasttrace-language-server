# OpenFastTrace Language Server 0.7, released 2026-08-18

Version 0.7 turns to the coverage links themselves. Raising a revision on an item now offers the fix right there instead of only on the tags it left behind, navigation from a `Covers:` entry leads to the item, and the extension is available from the Open VSX Registry.

## Features

* #52: every outdated reference to an item updated from the item itself, not only from a coverage tag

## Bug Fixes

* #49: go to definition from a `Covers:` entry leads to the item, also when it is declared in the same file
* #50: an item no longer suggested in its own `Covers:` section

## Maintenance

* #51: releases published to the Open VSX Registry
