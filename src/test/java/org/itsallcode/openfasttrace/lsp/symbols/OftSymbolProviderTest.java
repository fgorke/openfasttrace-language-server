package org.itsallcode.openfasttrace.lsp.symbols;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.lsp4j.SymbolKind;
import org.itsallcode.openfasttrace.api.core.Location;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.junit.jupiter.api.Test;

class OftSymbolProviderTest {

    private static SpecificationItem specItem(final String id, final String title) {
        return SpecificationItem.builder()
                .id(SpecificationItemId.parseId(id))
                .title(title)
                .description("")
                .location(Location.create("spec.md", 3))
                .build();
    }

    // [utest->req~symbol-naming~1]
    @Test
    void testGivenSpecificationItemWhenNamingThenIdIsUsed() {
        // given
        final SpecificationItem item = specItem("req~login~1", "Login");

        // when / then
        assertThat(OftSymbolProvider.nameOf(item)).isEqualTo("req~login~1");
        assertThat(OftSymbolProvider.detailOf(item)).isEqualTo("Login");
        assertThat(OftSymbolProvider.SYMBOL_KIND).isEqualTo(SymbolKind.Class);
    }

    // [utest->req~workspace-symbol-search~1]
    @Test
    void testGivenEmptyQueryWhenMatchingThenEverythingMatches() {
        // given
        final SpecificationItem item = specItem("req~login~1", "Login");

        // when / then
        assertThat(OftSymbolProvider.matches(item, "")).isTrue();
        assertThat(OftSymbolProvider.matches(item, "   ")).isTrue();
        assertThat(OftSymbolProvider.matches(item, null)).isTrue();
    }

    // [utest->req~workspace-symbol-search~1]
    @Test
    void testGivenQueryMatchingTitleWhenMatchingThenItemMatchesIgnoringCase() {
        // given
        final SpecificationItem item = specItem("req~abc~1", "Login Requirement");

        // when / then
        assertThat(OftSymbolProvider.matches(item, "login")).isTrue();
    }

    // [utest->req~workspace-symbol-search~1]
    @Test
    void testGivenSeveralItemsWhenFindingMatchesThenResultIsSortedByName() {
        // given
        final List<SpecificationItem> items = List.of(
                specItem("req~zulu~1", "Zulu"),
                specItem("req~alpha~1", "Alpha"),
                specItem("feat~mike~1", "Mike"));

        // when
        final List<SpecificationItem> matches = OftSymbolProvider.findMatching(items, "");

        // then
        assertThat(matches).extracting(OftSymbolProvider::nameOf)
                .containsExactly("feat~mike~1", "req~alpha~1", "req~zulu~1");
    }
}
