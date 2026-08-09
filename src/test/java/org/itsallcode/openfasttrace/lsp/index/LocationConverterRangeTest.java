package org.itsallcode.openfasttrace.lsp.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lsp4j.Range;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocationConverterRangeTest {

    @TempDir
    Path workspace;

    private OftWorkspaceIndex index;

    @BeforeEach
    void setUp() throws Exception {
        Files.write(workspace.resolve("spec.md"), List.of(
                "# Login", "", "`req~login~1`", "", "Needs: impl", ""));
        Files.write(workspace.resolve("Login.java"), List.of(
                "class Login {", "    // [impl->req~login~1]", "}"));
        index = new WorkspaceIndexer().buildIndex(workspace);
    }

    private SpecificationItem specItem() {
        return index.findSpecItem(SpecificationItemId.parseId("req~login~1")).orElseThrow();
    }

    // [itest->req~precise-ranges-from-oft~1]
    @Test
    void testGivenSpecItemWhenTakingItsRangeThenLineAndColumnAreZeroBased() {
        // given
        final SpecificationItem item = specItem();

        // when
        final Range range = LocationConverter.rangeOfDeclaredId(item).orElseThrow();

        // then
        assertThat(item.getLocation().getLine()).isEqualTo(3);
        assertThat(range.getStart().getLine()).isEqualTo(2);
        assertThat(range.getStart().getCharacter()).isEqualTo(1);
        assertThat(range.getEnd().getCharacter()).isEqualTo(1 + "req~login~1".length());
    }

    // [itest->req~precise-ranges-from-oft~1]
    @Test
    void testGivenCoverageTagWhenTakingItsRangeThenNoneIsReported() {
        // given
        final SpecificationItem tag = index
                .findCoverageTags(SpecificationItemId.parseId("req~login~1")).get(0);

        // when / then
        assertThat(LocationConverter.rangeOfDeclaredId(tag)).isEmpty();
    }

    // [itest->req~precise-ranges-from-oft~1]
    @Test
    void testGivenCoverageTagWhenTakingTheCoveredIdRangeThenItPointsAtTheIdentifier() {
        // given
        final SpecificationItem tag = index
                .findCoverageTags(SpecificationItemId.parseId("req~login~1")).get(0);

        // when
        final Range range = LocationConverter
                .toLspRange(tag.getLocatedCoveredIds().get(0).getRange()).orElseThrow();

        // then
        assertThat(range.getStart().getLine()).isEqualTo(1);
        assertThat(range.getStart().getCharacter())
                .isEqualTo("    // [impl->".length());
    }

    // [itest->req~precise-ranges-from-oft~1]
    @Test
    void testGivenNoRangeWhenConvertingThenNothingIsReturned() {
        assertThat(LocationConverter.toLspRange(null)).isEmpty();
    }
}
