---
status: accepted
date: 2026-07-05
decision-makers: Felix Gorke
---

# Write Given-When-Then Tests Traced with OFT

## Context and Problem Statement

The tests have to do more than catch regressions. A reader should be able to tell what a test checks and why. The test suite is also part of the traceability story the project is about. So a few things had to be settled: a consistent test structure, a naming scheme, a clear line between unit and integration tests and a way to tie each test back to the requirement it covers.

## Considered Options

Structure and naming:

* Given-When-Then bodies with `testGiven…When…Then…` method names
* Free-form bodies with short `method_scenario_result` names

Unit/integration split:

* Tag each test as a unit test (`utest`) or an integration test (`itest`) and make the requirement's `Needs` match
* Treat every test as one undifferentiated bucket

Traceability:

* Carry OFT coverage tags in the test comments and let OFT trace the suite against the requirements
* Leave tests untraced and rely on the requirement text alone

## Decision Outcome

Chosen: all three of the first options. Tests use Given-When-Then bodies. The unit/integration split is expressed through OFT artifact types. Coverage tags let OFT trace its own suite.

**Structure.** Every test body has three parts: `// given`, `// when`, `// then`. Arrange the inputs. Call the one method under test. Assert on the result. It reads the same everywhere. It also forces each test to have a single clear action instead of mixing setup and assertions.

**Naming.** Methods follow `testGiven<Context>When<Action>Then<Outcome>`. The names can get long but say exactly what broke without anyone opening the file.

**Frameworks.** JUnit 5 is the runner. AssertJ does the assertions (`assertThat(...).containsExactly(...)`). Mockito is used only where a real collaborator would be awkward: the `LanguageClient` callback and the `WorkspaceIndexer` in a couple of server tests. Most tests need no mocks at all. The domain classes take plain in-memory data.

**Unit vs integration.** A test is a unit test (`utest`) if it builds one class and calls it directly with in-memory data. It is an integration test (`itest`) if it crosses a real boundary. The boundaries here are the file system and the real OFT import pipeline.

**Self-tracing.** Every test that maps to a requirement carries a coverage tag in a comment above it. So the project traces its own test suite with the same tool it provides IDE support for.

### Consequences

* Good, because a reader can open any test and see in three sections what it checks.
* Good, because the unit/integration split is machine-checked. If a test's artifact type and the requirement's `Needs` disagree, OFT flags it. The two cannot drift apart unnoticed.
* Good, because the suite is a worked example of the traceability the thesis argues for. The project uses its own feature on itself.
* Bad, because the method names get long and sometimes clumsy.
* Bad, because the raw JSON-RPC probes are manual and live outside the build. They only run when someone remembers to run them.

### Confirmation

`mvn test` runs the full suite. An OFT trace over `doc/spec` and `src` has to show no uncovered `Needs` and no unwanted coverage. That check keeps the `utest` and `itest` tags honest against the requirements.
