# Demo Walkthrough: OpenFastTrace Language Server

This folder contains a small example project (`example/`) that shows every
feature of the language server. Some defects in the example
are intentional.

## Preparation

1. Install the plugin or extension (see the [main README](https://github.com/fgorke/openfasttrace-language-server/blob/main/README.md#quickstart)).
2. Open **only** the folder `doc/demo/example` as the project/workspace

The example is a coffee maker: features and requirements live in
`spec/system_requirements.md`, the design in `spec/design.md`, implementation
and test stubs in `src/` and a user manual in `manual.md`.

Keyboard shortcuts below: IntelliJ first, then VS Code.

## Steps

### 1. Syntax highlighting

Open `spec/system_requirements.md` and `src/CoffeeMaker.java`.

>**Expected:** item IDs (such as `req~start-brewing~1`), section keywords
(`Needs:`, `Covers:`) and coverage tags (such as `[impl->dsn~brew-cycle~1]`)
are colored.

### 2. Hover

In `src/CoffeeMaker.java`, hover over the target of the first coverage tag,
which is `dsn~brew-cycle~1`.

> **Expected:** title and description of the design item appear as a tooltip.

### 3. Go to definition

a) In the same tag, invoke *Go to Definition* on the target
(`Ctrl+B` / `F12`). 
>**Expected:** jump to the definition in `spec/design.md`.

b) There, on the definition line of the item, invoke *Go to Definition*
again. 
>**Expected:** a list of both coverage tags (implementation and test).

### 4. Find references

Invoke *Find References* on the ID `req~start-brewing~1` in
`spec/system_requirements.md` (`Alt+F7` / `Shift+F12`).

>**Expected:** every place that covers the requirement.

### 5. Symbol search

Search for the symbol `grind` (`Ctrl+Alt+Shift+N` / `Ctrl+T`).

>**Expected:** the design item `dsn~grind-beans~1`.

### 6. Diagnostics for the whole project

Open the problems view (`Alt+6` / `Ctrl+Shift+M`).

>**Expected:** seven entries from three files, in three severities.
>
>Four **errors**, where something already written is wrong:
>* the requirement `req~fill-level-warning~1` is **defined twice**, marked at both places
>* the tag targeting `dsn~milk-frother~1` points at an item that **does not exist**
>* the tag targeting `dsn~heat-water~1` references an **outdated revision**
>
>One **warning**, where the trace is only unfinished: the item
>`dsn~grind-beans~1` is missing its required **utest coverage**.
>
>Two **informations**, which are not the marked item's doing at all:
>`feat~brew-coffee~1` and `req~start-brewing~1` are fine in themselves, only the
>chain below them is broken. More information in the [OFT User Guide](https://github.com/itsallcode/openfasttrace/blob/main/doc/user_guide.md#transitive-defects).

### 7. Quick fix

In `src/CoffeeMaker.java`, go to the outdated tag
`[impl->dsn~heat-water~1]` and invoke the quick fix (`Alt+Enter` / `Ctrl+.`).

>**Expected:** the fix replaces the revision with `~2`. After saving, the
error disappears.

### 8. Code lens

Open `spec/design.md` and look at the lines above the items.

>**Expected:** above the brew cycle item it reads **covered by impl, utest**,
above the grind beans item **missing utest · covered by impl**. 

### 9. Coverage hierarchy

Open the type hierarchy on the ID `req~start-brewing~1`
(`Ctrl+H` / right click → *Show Type Hierarchy*).

>**Expected:** the full chain from the feature at the top down to the
coverage tags in the source code.

### 10. Code completion 

a) **Covers entry:** in `spec/design.md`, below the cup counter item, type
the following two lines (invoke completion with `Ctrl+Space` and accept the
suggestion `req~start-brewing~1`):

```
Covers:
* req~sta
```

b) **Tag target:** in `src/CoffeeMaker.java`, below the line
"Completion exercise area", type `// [impl->grind`, completion suggests
the grind beans design item and appends the closing bracket.

c) **Tag skeletons:** start a new comment line `// ` and press
`Ctrl+Space`. 
>**Expected:** a skeleton for every artifact type the workspace
needs. Close with `Esc`.

### 11. Rename

Invoke *Rename* on the name part of `dsn~brew-cycle~1` in `spec/design.md`
(`Shift+F6` / `F2`), new name: `brew-sequence`.

>**Expected:** the definition, the tag in `CoffeeMaker.java` and the tag in
`CoffeeMakerTest.java` change together. No new defect appears.

### 12. Trace report

IntelliJ: *Tools → Generate OpenFastTrace Report…* 

VS Code: command palette (`Ctrl+Shift+P`) → *OpenFastTrace: Generate Trace Report*.

Choose **Plain text, defects only**.

>**Self check:** exactly **4 direct defects** remain. The duplicate
definition, counted twice, the tag pointing at the item that does not exist
and the missing utest coverage. The quick fix from step 7 removed the outdated
one.

## Afterwards

Discard your changes or reset the project to the original state.