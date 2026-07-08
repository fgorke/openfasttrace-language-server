package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OftTextDocumentServiceCompletionTest {

    private OftTextDocumentService service;

    @BeforeEach
    void setUp() {
        service = new OftTextDocumentService();
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenCursorInCoversSectionWhenCompletingThenMatchingSpecItemsAreSuggested() {
        // given
        service.updateIndex(new OftWorkspaceIndex(List.of(
                specItem("dsn~auth-flow~1"),
                specItem("dsn~payment~1"))));
        final String lastLine = "  * dsn~auth-fl";
        final List<String> lines = List.of("req~login~1", "", "Covers:", lastLine);

        // when
        final List<CompletionItem> items = service.completionForPosition(lines, 3, lastLine.length(), true);

        // then
        assertThat(items).extracting(CompletionItem::getLabel).containsExactly("dsn~auth-flow~1");
        assertThat(items.get(0).getTextEdit().getLeft().getNewText()).isEqualTo("dsn~auth-flow~1");
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenCursorInOpenCoverageTagTargetWhenCompletingThenMatchingSpecItemsAreSuggested() {
        // given
        service.updateIndex(new OftWorkspaceIndex(List.of(coverableItem("req~login~1"))));
        final String line = "// [impl->req~lo";
        final List<String> lines = List.of(line);

        // when
        final List<CompletionItem> items = service.completionForPosition(lines, 0, line.length(), true);

        // then
        assertThat(items).extracting(CompletionItem::getLabel).containsExactly("req~login~1");
    }

    // [utest->req~complete-closing-bracket-for-coverage-tag~1]
    @Test
    void testGivenOpenTagWithoutClosingBracketWhenCompletingThenClosingBracketIsAppended() {
        // given
        service.updateIndex(new OftWorkspaceIndex(List.of(coverableItem("req~login~1"))));
        final String line = "// [impl->req~lo";
        final List<String> lines = List.of(line);

        // when
        final List<CompletionItem> items = service.completionForPosition(lines, 0, line.length(), true);

        // then
        assertThat(items.get(0).getTextEdit().getLeft().getNewText()).isEqualTo("req~login~1]");
    }

    // [utest->req~complete-closing-bracket-for-coverage-tag~1]
    @Test
    void testGivenClosingBracketAlreadyPresentWhenCompletingThenItIsNotDuplicated() {
        // given
        service.updateIndex(new OftWorkspaceIndex(List.of(coverableItem("req~login~1"))));
        final String line = "// [impl->req~lo]";
        final int col = line.indexOf("lo") + 2; // cursor right after "lo", before "]"
        final List<String> lines = List.of(line);

        // when
        final List<CompletionItem> items = service.completionForPosition(lines, 0, col, true);

        // then
        assertThat(items.get(0).getTextEdit().getLeft().getNewText()).isEqualTo("req~login~1");
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenCursorInOpenCoverageTagTargetWhenCompletingThenTextEditReplacesOnlyThePrefix() {
        // given
        service.updateIndex(new OftWorkspaceIndex(List.of(coverableItem("req~login~1"))));
        final String line = "// [impl->req~lo";
        final List<String> lines = List.of(line);

        // when
        final List<CompletionItem> items = service.completionForPosition(lines, 0, line.length(), true);

        // then
        final var range = items.get(0).getTextEdit().getLeft().getRange();
        assertThat(range.getStart().getCharacter()).isEqualTo(line.indexOf("req~lo"));
        assertThat(range.getEnd().getCharacter()).isEqualTo(line.length());
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenCursorOutsideAnyContextWhenCompletingThenNoSuggestionsAreReturned() {
        // given
        service.updateIndex(new OftWorkspaceIndex(List.of(specItem("req~login~1"))));
        final List<String> lines = List.of("plain source code, no tag here");

        // when
        final List<CompletionItem> items = service.completionForPosition(lines, 0, 10, true);

        // then
        assertThat(items).isEmpty();
    }

    // [utest->req~suggest-coverage-tag-start-in-comment~1]
    @Test
    void testGivenLineWithLoneAsteriskWhenCompletingThenNoSkeletonsAreSuggested() {
        // given
        service.updateIndex(OftWorkspaceIndex.empty());
        final String line = "int x = a * b";
        final List<String> lines = List.of(line);

        // when
        final List<CompletionItem> items = service.completionForPosition(lines, 0, line.length(), true);

        // then
        assertThat(items).isEmpty();
    }

    // [utest->req~suggest-coverage-tag-start-in-comment~1]
    @Test
    void testGivenManualInvocationInsideCommentWithoutTagWhenCompletingThenTagSkeletonsAreSuggested() {
        // given
        service.updateIndex(OftWorkspaceIndex.empty());
        final String line = "// ";
        final List<String> lines = List.of(line);

        // when
        final List<CompletionItem> items = service.completionForPosition(lines, 0, line.length(), true);

        // then
        assertThat(items).extracting(CompletionItem::getLabel)
                .containsExactlyInAnyOrder("[impl->...]", "[utest->...]", "[itest->...]", "[stest->...]");
        final var edit = items.get(0).getTextEdit().getLeft();
        assertThat(edit.getNewText()).matches("\\[\\w+->\\$0]");
        assertThat(edit.getRange().getStart()).isEqualTo(edit.getRange().getEnd());
    }

    // [utest->req~suggest-coverage-tag-start-in-comment~1]
    @Test
    void testGivenTriggerCharacterInvocationInsideCommentWhenCompletingThenNoTagStartIsSuggested() {
        // given
        service.updateIndex(OftWorkspaceIndex.empty());
        final String line = "// ";
        final List<String> lines = List.of(line);

        // when
        final List<CompletionItem> items = service.completionForPosition(lines, 0, line.length(), false);

        // then
        assertThat(items).isEmpty();
    }

    private static SpecificationItem specItem(final String id) {
        return SpecificationItem.builder()
                .id(SpecificationItemId.parseId(id))
                .title("Title")
                .description("")
                .build();
    }

    private static SpecificationItem coverableItem(final String id) {
        return SpecificationItem.builder()
                .id(SpecificationItemId.parseId(id))
                .title("Title")
                .description("")
                .addNeedsArtifactType("impl")
                .build();
    }
}
