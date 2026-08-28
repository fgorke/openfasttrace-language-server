package org.itsallcode.openfasttrace.lsp.decisions;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AdrItemIdActionTest {

    private static final List<String> RECORD = List.of(
            "# Cache the Workspace Index on Disk",
            "",
            "## Context and Problem Statement");

    private static String uri(final String path) {
        return Path.of(path).toUri().toString();
    }

    // [utest->req~generate-specification-item-id-for-adr~1]
    @Test
    void testGivenTitleOfRecordWithoutIdWhenAskingForTheEditThenIdIsInsertedBelowTheTitle() {
        // given
        final String uri = uri("doc/decisions/0001-cache-the-workspace-index-on-disk.md");

        // when
        final Optional<TextEdit> edit = AdrItemIdAction.idEditFor(uri, RECORD, 0);

        // then
        assertThat(edit).isPresent();
        assertThat(edit.get().getNewText())
                .isEqualTo("\n`adr~cache-the-workspace-index-on-disk~1`");
        assertThat(edit.get().getRange().getStart().getLine()).isZero();
    }

    // [utest->req~generate-specification-item-id-for-adr~1]
    @Test
    void testGivenUnnumberedFileNameWhenDerivingTheIdThenTheWholeNameIsKept() {
        // given
        final String uri = uri("doc/adr/cache-the-workspace-index.md");

        // when / then
        assertThat(AdrItemIdAction.idTextFor(uri)).contains("adr~cache-the-workspace-index~1");
    }


    // [utest->req~generate-specification-item-id-for-adr~1]
    @Test
    void testGivenRecordThatAlreadyHasAnIdWhenAskingForTheEditThenNothingIsOffered() {
        // given
        final String uri = uri("doc/decisions/0001-cache-the-workspace-index-on-disk.md");
        final List<String> withId = List.of(
                "# Cache the Workspace Index on Disk",
                "`adr~cache-the-workspace-index-on-disk~1`");

        // when / then
        assertThat(AdrItemIdAction.idEditFor(uri, withId, 0)).isEmpty();
    }

    // [utest->req~generate-specification-item-id-for-adr~1]
    @ParameterizedTest
    @CsvSource({ "2", "99" })
    void testGivenLineThatIsNotTheTitleWhenAskingForTheEditThenNothingIsOffered(final int line) {
        // given
        final String uri = uri("doc/decisions/0001-cache-the-workspace-index-on-disk.md");

        // when / then
        assertThat(AdrItemIdAction.idEditFor(uri, RECORD, line)).isEmpty();
    }

    // [utest->req~generate-specification-item-id-for-adr~1]
    @ParameterizedTest
    @CsvSource({
            "'doc/decisions/0001-x-y.md', true",
            "'doc/adr/0001-x-y.md', true",
            "'doc/Decisions/0001-x-y.markdown', true",
            "'doc/spec/0001-x-y.md', false",
            "'doc/decisions/0001-x-y.txt', false"
    })
    void testGivenFileWhenCheckingForDecisionRecordThenOnlyMarkdownInADecisionDirectoryCounts(
            final String path, final boolean expected) {
        assertThat(AdrItemIdAction.isDecisionRecord(uri(path))).isEqualTo(expected);
    }
}
