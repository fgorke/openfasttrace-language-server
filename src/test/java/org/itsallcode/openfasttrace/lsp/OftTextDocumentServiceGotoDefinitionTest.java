package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.lsp4j.Location;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OftTextDocumentServiceGotoDefinitionTest {

    private static final String SPEC_URI = "file:///workspace/spec.md";

    private OftTextDocumentService service;

    @BeforeEach
    void setUp() {
        service = new OftTextDocumentService();
    }

    // [utest->req~goto-definition-tag-to-spec~2]
    @Test
    void testGivenCursorOnCoverageTagInImplFileWhenGoingToDefinitionThenSpecItemLocationIsReturned() {
        // given
        final var specItem = specItemAt("req~my-req~1", "/workspace/spec.md", 9);
        final var coverageTag = coverageTagAt("impl~impl~0", "req~my-req~1",
                "/workspace/impl.md", 5);
        service.updateIndex(new OftWorkspaceIndex(List.of(specItem, coverageTag)));

        // when
        final var locations = service.definitionForLine("Covers: req~my-req~1", 9);

        // then
        assertThat(locations).hasSize(1);
        assertThat(locations.get(0).getUri()).isEqualTo(SPEC_URI);
        assertThat(locations.get(0).getRange().getStart().getLine()).isEqualTo(9);
    }

    // [utest->req~goto-definition-spec-to-tags~1]
    @Test
    void testGivenCursorOnSpecItemIdInSpecFileWhenGoingToDefinitionThenAllCoveringTagLocationsAreReturned() {
        // given
        final var specItem = specItemAt("req~my-req~1", "/workspace/spec.md", 10);
        final var tag1 = coverageTagAt("impl~mod-a~0", "req~my-req~1", "/workspace/a.md", 3);
        final var tag2 = coverageTagAt("impl~mod-b~0", "req~my-req~1", "/workspace/b.md", 7);
        service.updateIndex(new OftWorkspaceIndex(List.of(specItem, tag1, tag2)));

        // when
        final var locations = service.definitionForLine("`req~my-req~1`", 3);

        // then
        assertThat(locations).hasSize(2);
        final var uris = locations.stream().map(Location::getUri).toList();
        assertThat(uris).containsExactlyInAnyOrder(
                "file:///workspace/a.md", "file:///workspace/b.md");
    }

    // [utest->req~goto-definition-tag-to-spec~2]
    @Test
    void testGivenCursorOnCoversEntryNamingAnItemOfTheSameFileWhenGoingToDefinitionThenTheItemIsReturned() {
        // given
        final var covered = specItemAt("req~my-req~1", "/workspace/spec.md", 1);
        final var covering = specItemAt("dsn~my-design~1", "/workspace/spec.md", 4);
        service.updateIndex(new OftWorkspaceIndex(List.of(covered, covering)));

        // when
        final var locations = service.definitionForLine("* req~my-req~1", 4);

        // then
        assertThat(locations).singleElement().satisfies(location -> {
            assertThat(location.getUri()).isEqualTo(SPEC_URI);
            assertThat(location.getRange().getStart().getLine()).isEqualTo(1);
        });
    }

    // [utest->req~goto-definition-tag-to-spec~2]
    @Test
    void testGivenCursorOnUnknownIdWhenGoingToDefinitionThenNoLocationIsReturned() {
        // given
        service.updateIndex(OftWorkspaceIndex.empty());

        // when
        final var locations = service.definitionForLine("req~unknown~1", 0);

        // then
        assertThat(locations).isEmpty();
    }

    private SpecificationItem specItemAt(final String id, final String path, final int line) {
        return SpecificationItem.builder()
                .id(SpecificationItemId.parseId(id))
                .title("Title of " + id)
                .description("Description.")
                .location(org.itsallcode.openfasttrace.api.core.Location.create(path, line + 1))
                .build();
    }

    private SpecificationItem coverageTagAt(final String tagId, final String coveredId,
            final String path, final int line) {
        return SpecificationItem.builder()
                .id(SpecificationItemId.parseId(tagId))
                .title("")
                .description("")
                .location(org.itsallcode.openfasttrace.api.core.Location.create(path, line + 1))
                .addCoveredId(SpecificationItemId.parseId(coveredId))
                .build();
    }
}
