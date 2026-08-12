package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftTextDocumentServiceQuickFixTest {

    @TempDir
    Path workspace;

    private OftTextDocumentService service;
    private Path source;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(workspace.resolve("spec.md"), String.join("\n",
                "# Login", "", "`req~login~3`", "", "Needs: impl", "",
                "# Design", "", "`dsn~login~1`", "", "Covers:", "* req~login~1", ""));
        source = workspace.resolve("Login.java");
        Files.writeString(source, String.join("\n",
                "// [impl->req~login~1]",
                "// [impl->req~login~2]",
                "// [impl->req~other~1]") + "\n");
        service = new OftTextDocumentService();
        service.updateIndex(new WorkspaceIndexer().buildIndex(workspace));
    }

    private List<CodeAction> actionsForFirstOutdatedTag() throws Exception {
        final String uri = source.toUri().toString();
        final List<Diagnostic> diagnostics =
                new org.itsallcode.openfasttrace.lsp.diagnostics.DiagnosticsProvider()
                        .diagnoseFile(uri, Files.readAllLines(source), indexOfService());
        final Diagnostic outdated = diagnostics.stream()
                .filter(diagnostic -> diagnostic.getData() != null)
                .findFirst().orElseThrow();
        final var params = new CodeActionParams(new TextDocumentIdentifier(uri),
                outdated.getRange(), new CodeActionContext(List.of(outdated)));
        return service.codeAction(params).get().stream().map(either -> either.getRight()).toList();
    }

    private org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex indexOfService() {
        return new WorkspaceIndexer().buildIndex(workspace);
    }

    // [itest->req~quickfix-updates-all-versions~1]
    @Test
    void testGivenSeveralOutdatedReferencesWhenAskingForActionsThenOneUpdatesThemAll()
            throws Exception {
        // when
        final List<CodeAction> actions = actionsForFirstOutdatedTag();

        // then
        final CodeAction updateAll = actions.stream()
                .filter(action -> action.getTitle().startsWith("Update all"))
                .findFirst().orElseThrow();
        final Map<String, List<TextEdit>> changes = updateAll.getEdit().getChanges();
        assertThat(changes.values().stream().mapToInt(List::size).sum()).isEqualTo(3);
        assertThat(changes).hasSize(2);
        assertThat(changes.values().stream().flatMap(List::stream))
                .allSatisfy(edit -> assertThat(edit.getNewText()).isEqualTo("3"));
    }

    // [itest->req~quickfix-updates-all-versions~1]
    @Test
    void testGivenOneOutdatedReferenceWhenAskingForActionsThenNoBulkActionIsOffered()
            throws Exception {
        // given
        Files.writeString(workspace.resolve("spec.md"), String.join("\n",
                "# Login", "", "`req~login~3`", "", "Needs: impl", ""));
        Files.writeString(source, "// [impl->req~login~1]\n");
        service.updateIndex(new WorkspaceIndexer().buildIndex(workspace));

        // when
        final List<CodeAction> actions = actionsForFirstOutdatedTag();

        // then
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Update to req~login~3");
    }
}
