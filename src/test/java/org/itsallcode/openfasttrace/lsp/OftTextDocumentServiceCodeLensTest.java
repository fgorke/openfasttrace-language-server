package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftTextDocumentServiceCodeLensTest {

    @TempDir
    Path workspace;

    private OftTextDocumentService service;
    private Path spec;

    @BeforeEach
    void setUp() throws Exception {
        spec = workspace.resolve("spec.md");
        Files.writeString(spec, "# Login\n\n`req~login~1`\n\nNeeds: impl, utest\n");
        Files.writeString(workspace.resolve("Login.java"), "// [impl->req~login~1]\n");
        service = new OftTextDocumentService();
        service.updateIndex(new WorkspaceIndexer().buildIndex(workspace));
    }

    private List<? extends CodeLens> codeLensesFor(final Path file) throws Exception {
        final var params = new CodeLensParams(new TextDocumentIdentifier(file.toUri().toString()));
        return service.codeLens(params).get();
    }

    // [itest->req~coverage-code-lens~1]
    @Test
    void testGivenSpecFileWhenAskingTheServiceForCodeLensesThenCoverageIsSummarised()
            throws Exception {
        assertThat(codeLensesFor(spec)).singleElement().satisfies(lens -> {
            assertThat(lens.getCommand().getTitle()).isEqualTo("missing utest · covered by impl");
            assertThat(lens.getRange().getStart().getLine()).isEqualTo(2);
        });
    }

    // [itest->req~coverage-code-lens~1]
    @Test
    void testGivenFileWithoutSpecItemsWhenAskingTheServiceForCodeLensesThenNoneAreReturned()
            throws Exception {
        // given
        final Path plain = workspace.resolve("notes.md");
        Files.writeString(plain, "# Notes\n\nNothing traced here.\n");
        service.updateIndex(new WorkspaceIndexer().buildIndex(workspace));

        // when / then
        assertThat(codeLensesFor(plain)).isEmpty();
    }
}
