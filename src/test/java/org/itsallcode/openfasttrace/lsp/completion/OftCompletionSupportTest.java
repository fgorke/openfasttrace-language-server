package org.itsallcode.openfasttrace.lsp.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.completion.OftCompletionSupport.MatchKind;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.junit.jupiter.api.Test;

class OftCompletionSupportTest {

    private static SpecificationItem item(final String id) {
        return SpecificationItem.builder()
                .id(SpecificationItemId.parseId(id))
                .title("Title")
                .description("")
                .build();
    }

    private static SpecificationItem itemWithoutTitle(final String id) {
        return SpecificationItem.builder()
                .id(SpecificationItemId.parseId(id))
                .title("")
                .description("")
                .build();
    }

    private static SpecificationItem itemNeeding(final String id, final String... needs) {
        final var builder = SpecificationItem.builder()
                .id(SpecificationItemId.parseId(id))
                .title("Title")
                .description("");
        for (final String needed : needs) {
            builder.addNeedsArtifactType(needed);
        }
        return builder.build();
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenPrefixMatchingFullIdWhenClassifyingMatchThenFullIdPrefixIsReturned() {
        // given
        final SpecificationItem item = item("req~login~1");

        // when
        final MatchKind matchKind = OftCompletionSupport.matchKind(item, "req~lo");

        // then
        assertThat(matchKind).isEqualTo(MatchKind.FULL_ID_PREFIX);
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenPrefixMatchingNameStartWhenClassifyingMatchThenNamePrefixIsReturned() {
        // given
        final SpecificationItem item = item("req~login~1");

        // when
        final MatchKind matchKind = OftCompletionSupport.matchKind(item, "log");

        // then
        assertThat(matchKind).isEqualTo(MatchKind.NAME_PREFIX);
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenPrefixMatchingNameSubstringWhenClassifyingMatchThenNameSubstringIsReturned() {
        // given
        final SpecificationItem item = item("req~user-login~1");

        // when
        final MatchKind matchKind = OftCompletionSupport.matchKind(item, "log");

        // then
        assertThat(matchKind).isEqualTo(MatchKind.NAME_SUBSTRING);
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenPrefixMatchingArtifactTypeWhenClassifyingMatchThenFullIdPrefixIsReturnedBecauseArtifactTypeTierIsUnreachable() {
        // given
        final SpecificationItem item = item("req~login~1");

        // when
        final MatchKind matchKind = OftCompletionSupport.matchKind(item, "re");

        // then
        assertThat(matchKind).isEqualTo(MatchKind.FULL_ID_PREFIX);
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenPrefixMatchingNothingWhenClassifyingMatchThenNoneIsReturned() {
        // given
        final SpecificationItem item = item("req~login~1");

        // when
        final MatchKind matchKind = OftCompletionSupport.matchKind(item, "xyz");

        // then
        assertThat(matchKind).isEqualTo(MatchKind.NONE);
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenUppercasePrefixWhenClassifyingMatchThenMatchingIsCaseInsensitive() {
        // given
        final SpecificationItem item = item("req~login~1");

        // when
        final MatchKind matchKind = OftCompletionSupport.matchKind(item, "REQ~LO");

        // then
        assertThat(matchKind).isEqualTo(MatchKind.FULL_ID_PREFIX);
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenEmptyPrefixWhenClassifyingMatchThenEverythingMatchesAsFullIdPrefix() {
        // given
        final SpecificationItem item = item("req~login~1");

        // when
        final MatchKind matchKind = OftCompletionSupport.matchKind(item, "");

        // then
        assertThat(matchKind).isEqualTo(MatchKind.FULL_ID_PREFIX);
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenItemsMatchingAtDifferentTiersWhenFindingMatchesThenFullIdPrefixRanksBeforeNamePrefix() {
        // given
        final var index = new OftWorkspaceIndex(List.of(
                item("dsn~login-flow~1"),
                item("login~login~1")));

        // when
        final var result = OftCompletionSupport.findMatching(index, "login", null);

        // then
        assertThat(result).extracting(matched -> matched.getId().toString())
                .containsExactly("login~login~1", "dsn~login-flow~1");
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenItemsWithoutTitleWhenFindingMatchesThenTheyAreExcluded() {
        // given
        final var index = new OftWorkspaceIndex(List.of(
                item("req~login~1"),
                itemWithoutTitle("impl~login-1501782393~0")));

        // when
        final var result = OftCompletionSupport.findMatching(index, "log", null);

        // then
        assertThat(result).extracting(matched -> matched.getId().toString())
                .containsExactly("req~login~1");
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenCoveringArtifactTypeWhenFindingMatchesThenOnlyItemsNeedingThatTypeAreSuggested() {
        // given
        // Only items whose Needs list contains impl are valid [impl-> targets.
        final var index = new OftWorkspaceIndex(List.of(
                itemNeeding("req~login~1", "impl", "utest"),
                itemNeeding("dsn~login-flow~1", "impl"),
                itemNeeding("feat~login~1", "req")));

        // when
        final var result = OftCompletionSupport.findMatching(index, "log", "impl");

        // then
        assertThat(result).extracting(matched -> matched.getId().toString())
                .containsExactlyInAnyOrder("req~login~1", "dsn~login-flow~1");
    }

    // [utest->req~complete-specification-item-id-in-covers-section~1]
    @Test
    void testGivenItemsMatchingAtDifferentTiersWhenBuildingSortTextThenBetterMatchSortsFirst() {
        // given
        final String prefix = "log";

        // when
        final String idPrefixSort = OftCompletionSupport.sortTextFor(item("log~login~1"), prefix);
        final String nameSubstringSort =
                OftCompletionSupport.sortTextFor(item("dsn~user-login~1"), prefix);

        // then
        assertThat(idPrefixSort).isLessThan(nameSubstringSort);
    }
}
