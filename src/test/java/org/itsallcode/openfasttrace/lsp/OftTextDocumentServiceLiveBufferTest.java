package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftTextDocumentServiceLiveBufferTest {

    private OftTextDocumentService service;

    @BeforeEach
    void setUp() {
        service = new OftTextDocumentService();
        service.updateIndex(new OftWorkspaceIndex(List.of(
                SpecificationItem.builder()
                        .id(SpecificationItemId.parseId("req~my-req~1"))
                        .title("My Requirement")
                        .description("Desc.")
                        .addNeedsArtifactType("impl")
                        .build())));
    }

    // [utest->req~live-document-buffer~1]
    @Test
    void testGivenUnsavedOpenedContentWhenCompletingThenBufferContentIsUsed() throws Exception {
        // given
        final String uri = "file:///unsaved/probe.java";
        final String line = "// [impl->req~my-";
        service.didOpen(openParams(uri, line));

        // when
        final var items = service.completion(new CompletionParams(
                new TextDocumentIdentifier(uri), new Position(0, line.length())))
                .get().getLeft();

        // then
        assertThat(items).extracting(CompletionItem::getLabel).containsExactly("req~my-req~1");
    }

    // [utest->req~live-document-buffer~1]
    @Test
    void testGivenBufferReplacedByDidChangeWhenCompletingThenNewContentIsUsed() throws Exception {
        // given
        final String uri = "file:///unsaved/probe.java";
        service.didOpen(openParams(uri, "// nothing here yet"));
        final String updatedLine = "// [impl->req~my-";
        service.didChange(new DidChangeTextDocumentParams(
                new VersionedTextDocumentIdentifier(uri, 2),
                List.of(new TextDocumentContentChangeEvent(updatedLine))));

        // when
        final var items = service.completion(new CompletionParams(
                new TextDocumentIdentifier(uri), new Position(0, updatedLine.length())))
                .get().getLeft();

        // then
        assertThat(items).extracting(CompletionItem::getLabel).containsExactly("req~my-req~1");
    }

    // [itest->req~live-document-buffer~1]
    @Test
    void testGivenClosedDocumentWhenHoveringThenContentFallsBackToDisk(@TempDir final Path tempDir)
            throws Exception {
        // given
        final Path file = tempDir.resolve("probe.md");
        Files.writeString(file, "Covers: req~my-req~1");
        final String uri = file.toUri().toString();
        service.didOpen(openParams(uri, "Covers: req~unknown~1")); // different unsaved content
        service.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));

        // when
        final var hover = service.hover(new HoverParams(
                new TextDocumentIdentifier(uri), new Position(0, 9))).get();

        // then
        assertThat(hover).isNotNull();
        assertThat(hover.getContents().getRight().getValue()).contains("My Requirement");
    }

    // [utest->req~live-document-buffer~1]
    @Test
    void testGivenBufferedContentWithTrailingNewlineWhenReadingLinesThenLineNumberingMatchesDisk()
            throws Exception {
        // given
        final String uri = "file:///unsaved/multi.md";
        service.didOpen(openParams(uri, "req~my-req~1\n\nCovers: req~my-req~1\n"));

        // when
        final var hover = service.hover(new HoverParams(
                new TextDocumentIdentifier(uri), new Position(2, 9))).get();

        // then
        assertThat(hover).isNotNull();
    }

    private static DidOpenTextDocumentParams openParams(final String uri, final String text) {
        return new DidOpenTextDocumentParams(new TextDocumentItem(uri, "plaintext", 1, text));
    }
}
