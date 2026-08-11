package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftTextDocumentServiceIgnoreTest {

    @TempDir
    Path workspace;

    private OftTextDocumentService service;
    private Path walkthrough;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(workspace.resolve(".oftignore"), "docs\n");
        Files.writeString(workspace.resolve("spec.md"),
                "# Login\n\n`req~login~1`\n\nNeeds: impl\n");
        final Path docs = Files.createDirectories(workspace.resolve("docs"));
        walkthrough = docs.resolve("walkthrough.md");
        Files.writeString(walkthrough, "`req~login~1`\n\n<!-- [impl->req~login~1] -->\n");
        service = new OftTextDocumentService();
        service.updateIndex(new WorkspaceIndexer().buildIndex(workspace));
    }

    // [itest->req~index-ignore-file~1]
    @Test
    void testGivenAnExcludedFileWhenAskingForSemanticTokensThenNoneAreReported() throws Exception {
        // given
        final var params = new SemanticTokensParams(
                new TextDocumentIdentifier(walkthrough.toUri().toString()));

        // when / then
        assertThat(service.semanticTokensFull(params).get().getData()).isEmpty();
    }

    // [itest->req~index-ignore-file~1]
    @Test
    void testGivenAnExcludedFileWhenAskingForCompletionThenNothingIsSuggested() throws Exception {
        // given
        final var params = new CompletionParams(
                new TextDocumentIdentifier(walkthrough.toUri().toString()), new Position(2, 14));

        // when / then
        assertThat(service.completion(params).get().getLeft()).isEmpty();
    }
}
