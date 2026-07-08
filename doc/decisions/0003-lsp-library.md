---
status: accepted
date: 2026-05-13
decision-makers: Felix Gorke
---

# LSP Library: LSP4J 1.0.0

## Context and Problem Statement

The server needs a Java library that speaks the Language Server Protocol wire format. That lets the project focus on domain logic instead of JSON-RPC plumbing.

## Considered Options

* LSP4J (Eclipse)
* Hand-rolled JSON-RPC over stdio

## Decision Outcome

Chosen option: **LSP4J 1.0.0** (Feb 2026). It is the standard Java LSP implementation from the Eclipse Foundation. It supports LSP 3.18.

Entry points used:
- `LanguageServer`, implemented by `OftLanguageServer`
- `TextDocumentService`, handles `textDocument/*` requests
- `WorkspaceService`, handles `workspace/*` notifications
- `Launcher.createServerLauncher(...)`, wires up the stdio transport

### Consequences

* Good, because all JSON-RPC serialisation, LSP types and async dispatch are handled for us.
* Good, because it is current with LSP 3.18.
* Good, because future clients (IntelliJ in M2, VSCode in M3) can connect to the same server unchanged.
* Bad, because it drags in Eclipse dependencies (Xtend runtime, Guava) that bloat the fat JAR.

## More Information

- LSP4J releases: https://github.com/eclipse-lsp4j/lsp4j/releases
- LSP specification 3.18: https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/
