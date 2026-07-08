---
status: accepted
date: 2026-05-13
decision-makers: Felix Gorke
---

# License: GPL-3.0-or-later

## Context and Problem Statement

OpenFastTrace is licensed under GPL-3.0 without a Classpath Exception. This project links against OFT on the classpath. Under GPL terms the license propagates. There is no legal way around it.

## Considered Options

* GPL-3.0-or-later, the only option compatible with OFT's license
* Any other license, not viable given the GPL propagation above

## Decision Outcome

This project is licensed under **GPL-3.0-or-later**. This is a compliance requirement, not a design choice.

Evidence of OFT's license: https://github.com/itsallcode/openfasttrace/blob/main/LICENSE

### Consequences

* Neutral, because the project was always meant to be open source anyway.
* Good, because GPL-3.0-or-later is compatible with OFT's license. It satisfies the copyleft requirement.
* Bad, because anyone embedding this server as a library also has to go GPL-3.0-or-later or obtain a separate license.

### Confirmation

`LICENSE` contains the full GPL-3.0 text. Every source file starts with `// SPDX-License-Identifier: GPL-3.0-or-later`. The license is stated in `README.md`.
