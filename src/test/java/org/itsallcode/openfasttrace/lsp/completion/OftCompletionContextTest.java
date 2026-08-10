package org.itsallcode.openfasttrace.lsp.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class OftCompletionContextTest {

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenCursorInCoversSectionWhenFindingContextThenTypedPrefixIsReturned() {
        // given
        final String lastLine = "  * dsn~auth-fl";
        final List<String> lines = List.of(
                "req~login~1",
                "",
                "Covers:",
                lastLine);

        // when
        final var result = OftCompletionContext.findAt(lines, 3, lastLine.length());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().prefix()).isEqualTo("dsn~auth-fl");
        assertThat(result.get().coveringArtifactType()).isNull();
        assertThat(result.get().appendClosingBracket()).isFalse();
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenTagSourceWithNameAndRevisionWhenFindingContextThenArtifactTypeOnlyIsCaptured() {
        // given
        final String line = "// [impl~validate~2->dsn~va";
        final List<String> lines = List.of(line);

        // when
        final var result = OftCompletionContext.findAt(lines, 0, line.length());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().coveringArtifactType()).isEqualTo("impl");
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenCursorAfterCommaInTagWhenFindingContextThenAFreshPrefixIsCaptured() {
        // given
        final String line = "// [impl->req~login~1, ";
        final List<String> lines = List.of(line);

        // when
        final var result = OftCompletionContext.findAt(lines, 0, line.length());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().prefix()).isEmpty();
        assertThat(result.get().coveringArtifactType()).isEqualTo("impl");
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenCursorInSecondIdOfTagWhenFindingContextThenOnlyThatIdIsThePrefix() {
        // given
        final String line = "// [impl->req~login~1, req~log";
        final List<String> lines = List.of(line);

        // when
        final var result = OftCompletionContext.findAt(lines, 0, line.length());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().prefix()).isEqualTo("req~log");
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenCoversSectionEndedByOtherKeywordWhenFindingContextThenNoContextIsReturned() {
        // given
        final String lastLine = "so";
        final List<String> lines = List.of(
                "req~login~1",
                "Covers:",
                "  * dsn~auth-flow~1",
                "Needs: impl",
                lastLine);

        // when
        final var result = OftCompletionContext.findAt(lines, 4, lastLine.length());

        // then
        assertThat(result).isEmpty();
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenCoversSectionEndedByBacktickedDefinitionWhenFindingContextThenNoContextIsReturned() {
        // given
        final String lastLine = "prose of the next item";
        final List<String> lines = List.of(
                "Covers:",
                "  * dsn~auth-flow~1",
                "`dsn~auth-flow~1`",
                lastLine);

        // when
        final var result = OftCompletionContext.findAt(lines, 3, lastLine.length());

        // then
        assertThat(result).isEmpty();
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenBlankLinesInsideCoversSectionWhenFindingContextThenStillInsideSection() {
        // given
        final String lastLine = "  * dsn~a";
        final List<String> lines = List.of(
                "Covers:",
                "",
                lastLine);

        // when
        final var result = OftCompletionContext.findAt(lines, 2, lastLine.length());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().prefix()).isEqualTo("dsn~a");
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenOpenCoverageTagWithCommentMarkerWhenFindingContextThenTypedPrefixIsReturned() {
        // given
        final String line = "// [impl->req~lo";
        final List<String> lines = List.of(line);

        // when
        final var result = OftCompletionContext.findAt(lines, 0, line.length());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().prefix()).isEqualTo("req~lo");
        assertThat(result.get().coveringArtifactType()).isEqualTo("impl");
        assertThat(result.get().appendClosingBracket()).isTrue();
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenOpenCoverageTagWithoutCommentMarkerWhenFindingContextThenNoContextIsReturned() {
        // given
        final String line = "[impl->req~lo";
        final List<String> lines = List.of(line);

        // when
        final var result = OftCompletionContext.findAt(lines, 0, line.length());

        // then
        assertThat(result).isEmpty();
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenClosedCoverageTagWhenFindingContextThenNoContextIsReturned() {
        // given
        final String line = "// [impl->req~login~1] some more text";
        final List<String> lines = List.of(line);

        // when
        final var result = OftCompletionContext.findAt(lines, 0, line.length());

        // then
        assertThat(result).isEmpty();
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenCppLambdaReturnTypeWhenFindingContextThenNoContextIsReturned() {
        // given
        final String line = "// [this]() -> bool { return tr";
        final List<String> lines = List.of(line);

        // when
        final var result = OftCompletionContext.findAt(lines, 0, line.length());

        // then
        assertThat(result).isEmpty();
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenCursorRightAfterArrowWhenFindingContextThenEmptyPrefixIsReturned() {
        // given
        final String line = "// [impl->";
        final List<String> lines = List.of(line);

        // when
        final var result = OftCompletionContext.findAt(lines, 0, line.length());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().prefix()).isEmpty();
    }

    // [utest->req~suggest-coverage-tag-start-in-comment~2]
    @Test
    void testGivenCursorAlreadyInsideOpenTagWhenCheckingForOpenTagThenResultIsFalse() {
        // given
        final String line = "// [impl->req~lo";

        // when
        final boolean result = OftCompletionContext.isInsideCommentWithoutOpenTag(line, line.length());

        // then
        assertThat(result).isFalse();
    }
}
