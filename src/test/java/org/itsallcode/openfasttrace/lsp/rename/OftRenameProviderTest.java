package org.itsallcode.openfasttrace.lsp.rename;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

class OftRenameProviderTest {

    // [utest->req~prepare-rename~1]
    @Test
    void testGivenCursorOnIdWhenAskingForNameRangeThenOnlyTheNameIsCovered() {
        // given
        final String line = "`req~login~1`";

        // when
        final Optional<Range> range = OftRenameProvider.nameRangeAt(line, 0, 6);

        // then
        assertThat(range).isPresent();
        assertThat(line.substring(range.get().getStart().getCharacter(),
                range.get().getEnd().getCharacter())).isEqualTo("login");
    }

    // [utest->req~prepare-rename~1]
    @Test
    void testGivenCursorInsideCoverageTagWhenAskingForNameRangeThenTargetNameIsCovered() {
        // given
        final String line = "// [impl->req~login~1]";

        // when
        final Optional<Range> range = OftRenameProvider.nameRangeAt(line, 0, line.indexOf("login"));

        // then
        assertThat(range).isPresent();
        assertThat(line.substring(range.get().getStart().getCharacter(),
                range.get().getEnd().getCharacter())).isEqualTo("login");
    }

    // [utest->req~prepare-rename~1]
    @Test
    void testGivenCursorOutsideAnyIdWhenAskingForNameRangeThenNothingIsReturned() {
        // when
        final Optional<Range> range = OftRenameProvider.nameRangeAt("just some text", 0, 5);

        // then
        assertThat(range).isEmpty();
    }

    // [utest->req~rename-name-part-only~1]
    @Test
    void testGivenBareNameWhenExtractingThenItIsUsedAsIs() {
        assertThat(OftRenameProvider.extractItemName("authentication")).isEqualTo("authentication");
    }

    // [utest->req~rename-name-part-only~1]
    @Test
    void testGivenCompleteIdWhenExtractingThenOnlyTheNameIsTaken() {
        assertThat(OftRenameProvider.extractItemName("req~authentication~1"))
                .isEqualTo("authentication");
    }

    // [utest->req~rename-name-part-only~1]
    @Test
    void testGivenValidNamesWhenValidatingThenTheyAreAccepted() {
        assertThat(OftRenameProvider.isValidItemName("authentication")).isTrue();
        assertThat(OftRenameProvider.isValidItemName("user-login")).isTrue();
        assertThat(OftRenameProvider.isValidItemName("md.item-format")).isTrue();
    }

    // [utest->req~rename-name-part-only~1]
    @Test
    void testGivenInvalidNamesWhenValidatingThenTheyAreRejected() {
        assertThat(OftRenameProvider.isValidItemName("")).isFalse();
        assertThat(OftRenameProvider.isValidItemName("with space")).isFalse();
        assertThat(OftRenameProvider.isValidItemName("1starts-with-digit")).isFalse();
        assertThat(OftRenameProvider.isValidItemName("has~tilde")).isFalse();
    }

    // [utest->req~rename-specification-item~1]
    @Test
    void testGivenDefinitionLineWhenRenamingThenTheNameIsReplaced() {
        // given
        final String line = "`req~login~1`";

        // when
        final List<TextEdit> edits =
                OftRenameProvider.renameEditsInLine(line, 4, "req", "login", "authentication");

        // then
        assertThat(edits).hasSize(1);
        assertThat(edits.get(0).getNewText()).isEqualTo("authentication");
        assertThat(edits.get(0).getRange().getStart().getLine()).isEqualTo(4);
        assertThat(edits.get(0).getRange().getStart().getCharacter()).isEqualTo(5);
        assertThat(edits.get(0).getRange().getEnd().getCharacter()).isEqualTo(10);
    }

    // [utest->req~rename-specification-item~1]
    @Test
    void testGivenSeveralOccurrencesInOneLineWhenRenamingThenAllAreReplaced() {
        // given
        final String line = "* req~login~1 and req~login~2";

        // when
        final List<TextEdit> edits =
                OftRenameProvider.renameEditsInLine(line, 0, "req", "login", "auth");

        // then
        assertThat(edits).hasSize(2);
    }
}
