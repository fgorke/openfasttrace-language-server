package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.itsallcode.openfasttrace.api.core.Location;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OftWorkspaceServiceSymbolTest {

    private OftWorkspaceService service;

    @BeforeEach
    void setUp() {
        service = new OftWorkspaceService();
    }

    private static SpecificationItem specItem(final String id, final String title, final String path) {
        return SpecificationItem.builder()
                .id(SpecificationItemId.parseId(id))
                .title(title)
                .description("")
                .location(Location.create(path, 3))
                .build();
    }

    // [utest->req~workspace-symbol-search~1]
    @Test
    void testGivenEmptyQueryWhenSearchingSymbolsThenAllItemsAreReturned() {
        // given
        service.updateIndex(new OftWorkspaceIndex(List.of(
                specItem("req~login~1", "Login", "doc/spec.md"),
                specItem("feat~auth~1", "Authentication", "doc/features.md"))));

        // when
        final List<SymbolInformation> symbols = service.symbolsMatching("");

        // then
        assertThat(symbols).extracting(SymbolInformation::getName)
                .containsExactly("feat~auth~1", "req~login~1");
    }

    // [utest->req~workspace-symbol-search~1]
    @Test
    void testGivenQueryWhenSearchingSymbolsThenOnlyMatchingItemsAreReturned() {
        // given
        service.updateIndex(new OftWorkspaceIndex(List.of(
                specItem("req~login~1", "Login", "doc/spec.md"),
                specItem("feat~auth~1", "Authentication", "doc/features.md"))));

        // when
        final List<SymbolInformation> symbols = service.symbolsMatching("login");

        // then
        assertThat(symbols).extracting(SymbolInformation::getName).containsExactly("req~login~1");
    }

    // [utest->req~workspace-symbol-search~1]
    @Test
    void testGivenSpecificationItemWhenSearchingSymbolsThenLocationAndKindAreReported() {
        // given
        service.updateIndex(new OftWorkspaceIndex(
                List.of(specItem("req~login~1", "Login", "doc/spec.md"))));

        // when
        final SymbolInformation symbol = service.symbolsMatching("").get(0);

        // then
        assertThat(symbol.getKind()).isEqualTo(SymbolKind.Class);
        assertThat(symbol.getContainerName()).isEqualTo("Login");
        assertThat(symbol.getLocation().getUri()).endsWith("spec.md");
        assertThat(symbol.getLocation().getRange().getStart().getLine()).isEqualTo(2);
    }

    // [utest->req~workspace-symbol-search~1]
    @Test
    void testGivenEmptyIndexWhenSearchingSymbolsThenNothingIsReturned() {
        // given
        service.updateIndex(OftWorkspaceIndex.empty());

        // when
        final List<SymbolInformation> symbols = service.symbolsMatching("anything");

        // then
        assertThat(symbols).isEmpty();
    }
}
