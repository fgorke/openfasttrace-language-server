package org.itsallcode.openfasttrace.lsp.highlighting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class OftSemanticTokensProviderTest {

    // [utest->req~highlight-specification-item~1]
    @Test
    void testGivenLineWithOnlySpecificationItemIdWhenComputingTokensThenTypeTokenIsEmitted() {
        // given
        final List<String> lines = List.of("req~login~1");

        // when
        final List<Integer> data = OftSemanticTokensProvider.computeTokens(lines);

        // then
        // [deltaLine, deltaStartChar, length, tokenType, tokenModifiers]
        assertThat(data).containsExactly(0, 0, 11, 0, 0);
    }

    // [utest->req~highlight-specification-item~1]
    @Test
    void testGivenBacktickedSpecificationItemIdWhenComputingTokensThenTypeTokenCoversBareId() {
        // given
        final List<String> lines = List.of("`req~login~1`");

        // when
        final List<Integer> data = OftSemanticTokensProvider.computeTokens(lines);

        // then
        assertThat(data).containsExactly(0, 1, 11, 0, 0);
    }

    // [utest->req~highlight-specification-item~1]
    @Test
    void testGivenSpecificationItemIdWithSurroundingTextWhenComputingTokensThenNoTypeTokenIsEmitted() {
        // given
        final List<String> lines = List.of("See req~login~1 for details");

        // when
        final List<Integer> data = OftSemanticTokensProvider.computeTokens(lines);

        // then
        assertThat(data).isEmpty();
    }

    // [utest->req~highlight-keyword~1]
    @Test
    void testGivenCoversKeywordLineWhenComputingTokensThenKeywordTokenIsEmitted() {
        // given
        final List<String> lines = List.of("Covers:");

        // when
        final List<Integer> data = OftSemanticTokensProvider.computeTokens(lines);

        // then
        assertThat(data).containsExactly(0, 0, 6, 1, 0);
    }

    // [utest->req~highlight-keyword~1]
    @Test
    void testGivenIndentedKeywordWhenComputingTokensThenKeywordTokenIsEmittedAtIndentedOffset() {
        // given
        final List<String> lines = List.of("  Needs: impl");

        // when
        final List<Integer> data = OftSemanticTokensProvider.computeTokens(lines);

        // then
        assertThat(data).containsExactly(0, 2, 5, 1, 0);
    }

    // [utest->req~highlight-coverage-tag~1]
    @Test
    void testGivenCoverageTagWhenComputingTokensThenCoverageTagTokenSpansEntireTag() {
        // given
        final String line = "// [impl->req~login~1]";

        // when
        final List<Integer> data = OftSemanticTokensProvider.computeTokens(List.of(line));

        // then
        final int expectedLength = "[impl->req~login~1]".length();
        assertThat(data).containsExactly(0, 3, expectedLength, 2, 0);
    }

    // [utest->req~highlight-coverage-tag~1]
    @Test
    void testGivenTagCoveringSeveralItemsWhenComputingTokensThenTheWholeTagIsOneToken() {
        // given
        final String tag = "[impl->req~login~1, req~logout~2]";
        final String line = "// " + tag;

        // when
        final List<Integer> data = OftSemanticTokensProvider.computeTokens(List.of(line));

        // then
        assertThat(data).containsExactly(0, 3, tag.length(), 2, 0);
    }

    // [utest->req~highlight-coverage-tag~1]
    @Test
    void testGivenNamedTagCoveringSeveralItemsWhenComputingTokensThenTheWholeTagIsOneToken() {
        // given
        final String tag = "[impl~login-flow~1 -> req~login~1 , req~logout~2]";
        final String line = "# " + tag;

        // when
        final List<Integer> data = OftSemanticTokensProvider.computeTokens(List.of(line));

        // then
        assertThat(data).containsExactly(0, 2, tag.length(), 2, 0);
    }

    // [utest->req~highlight-coverage-tag~1]
    @Test
    void testGivenMultipleLinesWithTokensWhenComputingTokensThenDeltasAreEncodedRelatively() {
        // given
        final List<String> lines = List.of(
                "req~login~1",
                "",
                "Covers: dsn~auth-flow~1");

        // when
        final List<Integer> data = OftSemanticTokensProvider.computeTokens(lines);

        // then
        assertThat(data).containsExactly(
                0, 0, 11, 0, 0,
                2, 0, 6, 1, 0);
    }

}
