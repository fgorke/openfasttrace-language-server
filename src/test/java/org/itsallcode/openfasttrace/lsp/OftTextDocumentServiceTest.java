package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.itsallcode.openfasttrace.api.core.ItemStatus;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OftTextDocumentServiceTest {

    private OftTextDocumentService service;

    @BeforeEach
    void setUp() {
        service = new OftTextDocumentService();
    }

    // [utest->req~hover-specification-item~1]
    @Test
    void testGivenCursorOnCoverageTagWhenHoveringThenSpecItemTitleAndDescriptionAreReturned() {
        // given
        final var specItem = SpecificationItem.builder()
                .id(SpecificationItemId.parseId("req~my-req~1"))
                .title("My Requirement")
                .description("This is the description.")
                .build();
        service.updateIndex(new OftWorkspaceIndex(List.of(specItem)));
        final String line = "Covers: req~my-req~1";

        // when
        final var result = service.hoverForLine(line, 9, 0);

        // then
        assertThat(result).isPresent();
        final String hoverText = result.get().getContents().getRight().getValue();
        assertThat(hoverText)
                .contains("My Requirement")
                .contains("This is the description.");
    }

    // [utest->req~hover-specification-item~1]
    @Test
    void testGivenItemWithSectionsWhenBuildingTheHoverThenEachSectionKeywordIsEmphasised() {
        // given
        final var specItem = SpecificationItem.builder()
                .id(SpecificationItemId.parseId("req~my-req~1"))
                .title("My Requirement")
                .description("A description.")
                .rationale("A reason.")
                .comment("A remark.")
                .addCoveredId(SpecificationItemId.parseId("feat~my-feat~1"))
                .addDependOnId(SpecificationItemId.parseId("req~other~1"))
                .addTag("safety")
                .status(ItemStatus.DRAFT)
                .build();

        // when
        final String markdown = OftTextDocumentService.hoverMarkdown(specItem);

        // then
        assertThat(markdown)
                .contains("**My Requirement**")
                .contains("A description.")
                .contains("**Status:** draft")
                .contains("**Rationale:** A reason.")
                .contains("**Comment:** A remark.")
                .contains("**Depends:** req~other~1")
                .contains("**Tags:** safety")
                .doesNotContain("Covers");
    }

    // [utest->req~hover-specification-item~1]
    @Test
    void testGivenItemWithoutSectionsWhenBuildingTheHoverThenOnlyTitleAndDescriptionAppear() {
        // given
        final var specItem = SpecificationItem.builder()
                .id(SpecificationItemId.parseId("req~my-req~1"))
                .title("My Requirement")
                .description("A description.")
                .addNeedsArtifactType("impl")
                .build();

        // when
        final String markdown = OftTextDocumentService.hoverMarkdown(specItem);

        // then
        assertThat(markdown)
                .isEqualTo("**My Requirement**\n\nA description.")
                .doesNotContain("Needs")
                .doesNotContain("Status");
    }

    // [utest->req~hover-specification-item~1]
    @Test
    void testGivenCursorOnIdWithSeparatorsWhenHoveringThenTheRangeSpansTheWholeId() {
        // given
        final var specItem = SpecificationItem.builder()
                .id(SpecificationItemId.parseId("req~my-req~1"))
                .title("My Requirement")
                .description("This is the description.")
                .build();
        service.updateIndex(new OftWorkspaceIndex(List.of(specItem)));
        final String line = "// [impl->req~my-req~1]";
        final int insideTheWordMy = line.indexOf("my") + 1;

        // when
        final var result = service.hoverForLine(line, insideTheWordMy, 4);

        // then
        final var range = result.orElseThrow().getRange();
        assertThat(range.getStart().getLine()).isEqualTo(4);
        assertThat(range.getStart().getCharacter()).isEqualTo(line.indexOf("req~"));
        assertThat(range.getEnd().getCharacter()).isEqualTo(line.indexOf("]"));
    }

    // [utest->req~hover-specification-item~1]
    @Test
    void testGivenCursorNotOnAnyIdWhenHoveringThenNoHoverIsReturned() {
        // given
        service.updateIndex(OftWorkspaceIndex.empty());

        // when
        final var result = service.hoverForLine("plain text", 0, 0);

        // then
        assertThat(result).isEmpty();
    }

    // [utest->req~hover-specification-item~1]
    @Test
    void testGivenCursorOnUnknownIdWhenHoveringThenNoHoverIsReturned() {
        // given
        service.updateIndex(OftWorkspaceIndex.empty());

        // when
        final var result = service.hoverForLine("req~unknown~1", 0, 0);

        // then
        assertThat(result).isEmpty();
    }

    // [utest->req~index-refresh-on-file-change~1]
    @Test
    void testGivenTwoOpenFilesWhenIndexIsUpdatedThenDiagnosticsAreRepublishedForBoth() {
        // given
        final LanguageClient client = mock(LanguageClient.class);
        final List<PublishDiagnosticsParams> published = new ArrayList<>();
        doAnswer(inv -> { published.add(inv.getArgument(0)); return null; })
                .when(client).publishDiagnostics(any());
        service.connect(client);
        service.didOpen(openParams("file:///spec.md"));
        service.didOpen(openParams("file:///source.java"));
        published.clear();

        // when
        service.updateIndex(OftWorkspaceIndex.empty());

        // then
        assertThat(published).extracting(PublishDiagnosticsParams::getUri)
                .containsExactlyInAnyOrder("file:///spec.md", "file:///source.java");
    }

    // [utest->req~index-refresh-on-file-change~1]
    @Test
    void testGivenAClosedFileWhenIndexIsUpdatedThenNoDiagnosticsArePublishedForIt() {
        // given
        final LanguageClient client = mock(LanguageClient.class);
        service.connect(client);
        service.didOpen(openParams("file:///spec.md"));
        service.didClose(closeParams("file:///spec.md"));
        reset(client);

        // when
        service.updateIndex(OftWorkspaceIndex.empty());

        // then
        verify(client, never()).publishDiagnostics(any());
    }

    private static DidOpenTextDocumentParams openParams(final String uri) {
        final var item = new TextDocumentItem(uri, "plaintext", 1, "");
        return new DidOpenTextDocumentParams(item);
    }

    private static DidCloseTextDocumentParams closeParams(final String uri) {
        return new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri));
    }
}
