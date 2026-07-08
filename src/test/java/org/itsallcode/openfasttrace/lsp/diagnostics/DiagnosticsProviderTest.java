package org.itsallcode.openfasttrace.lsp.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.lsp4j.DiagnosticSeverity;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DiagnosticsProviderTest {

    private DiagnosticsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DiagnosticsProvider();
    }

    // [utest->req~diagnostic-outdated-version~1]
    @Test
    void testGivenCoverageTagWithOutdatedRevisionWhenDiagnosingThenWarningIsEmitted() {
        // given
        final var specItem = specItemAt("req~my-req~3", "/spec.md", 1);
        final String fileLine = "  - req~my-req~1";

        // when
        final var diagnostics = provider.diagnoseLines(
                List.of(fileLine), new OftWorkspaceIndex(List.of(specItem)));

        // then
        assertThat(diagnostics).hasSize(1);
        final var diagnostic = diagnostics.get(0);
        assertThat(diagnostic.getSeverity()).isEqualTo(DiagnosticSeverity.Warning);
        assertThat(diagnostic.getMessage().getLeft())
                .contains("req~my-req~1")
                .contains("3");
    }

    // [utest->req~diagnostic-outdated-version~1]
    @Test
    void testGivenCoverageTagWithUpToDateRevisionWhenDiagnosingThenNoDiagnosticIsEmitted() {
        // given
        final var specItem = specItemAt("req~my-req~3", "/spec.md", 1);
        final String fileLine = "  - req~my-req~3";

        // when
        final var diagnostics = provider.diagnoseLines(
                List.of(fileLine), new OftWorkspaceIndex(List.of(specItem)));

        // then
        assertThat(diagnostics).isEmpty();
    }

    // [utest->req~diagnostic-outdated-version~1]
    @Test
    void testGivenCoverageTagForUnknownIdWhenDiagnosingThenNoDiagnosticIsEmitted() {
        // given
        final String fileLine = "  - req~unknown~1";

        // when
        final var diagnostics = provider.diagnoseLines(
                List.of(fileLine), OftWorkspaceIndex.empty());

        // then
        assertThat(diagnostics).isEmpty();
    }

    // [utest->req~diagnostic-outdated-version~1]
    @Test
    void testGivenMultipleLinesWithMixedRevisionsWhenDiagnosingThenOnlyOutdatedLineIsFlagged() {
        // given
        final var req1 = specItemAt("req~foo~2", "/spec.md", 1);
        final var req2 = specItemAt("req~bar~5", "/spec.md", 3);
        final List<String> lines = List.of(
                "Covers: req~foo~2",
                "Covers: req~bar~1");

        // when
        final var diagnostics = provider.diagnoseLines(
                lines, new OftWorkspaceIndex(List.of(req1, req2)));

        // then
        assertThat(diagnostics).hasSize(1);
        assertThat(diagnostics.get(0).getRange().getStart().getLine()).isEqualTo(1);
    }

    private SpecificationItem specItemAt(final String id, final String path, final int line) {
        return SpecificationItem.builder()
                .id(SpecificationItemId.parseId(id))
                .title("Title")
                .description("Desc")
                .location(org.itsallcode.openfasttrace.api.core.Location.create(path, line))
                .build();
    }
}
