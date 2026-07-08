package org.itsallcode.openfasttrace.lsp.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceIndexerTest {

    private WorkspaceIndexer indexer;

    @BeforeEach
    void setUp() {
        indexer = new WorkspaceIndexer();
    }

    private Path testWorkspacePath() throws URISyntaxException {
        return Paths.get(getClass().getClassLoader().getResource("testworkspace").toURI());
    }

    // [itest->req~index-on-startup~1]
    @Test
    void testGivenWorkspaceWithSpecFilesWhenBuildingIndexThenSpecItemsAreLoaded()
            throws URISyntaxException {
        // given
        final Path workspace = testWorkspacePath();

        // when
        final var index = indexer.buildIndex(workspace);

        // then
        final var id = SpecificationItemId.parseId("feat~test-feature~1");
        final var item = index.findSpecItem(id).orElseThrow();
        assertThat(item.getTitle()).isEqualTo("Test Feature");
        assertThat(item.getDescription()).contains("testing the workspace indexer");
    }

    // [itest->req~index-on-startup~1]
    @Test
    void testGivenWorkspaceWithSourceCoverageTagsWhenBuildingIndexThenCoverageTagsAreIndexed()
            throws URISyntaxException {
        // given
        final Path workspace = testWorkspacePath();

        // when
        final var index = indexer.buildIndex(workspace);

        // then
        final var coveredId = SpecificationItemId.parseId("req~test-requirement~2");
        final var tags = index.findCoverageTags(coveredId);
        assertThat(tags).hasSize(1);
        assertThat(tags.get(0).getId().getArtifactType()).isEqualTo("impl");
    }

    // [itest->req~index-on-startup~1]
    @Test
    void testGivenEmptyWorkspaceDirectoryWhenBuildingIndexThenIndexIsEmpty(@TempDir final Path emptyDir) {
        // given / when
        final var index = indexer.buildIndex(emptyDir);

        // then
        assertThat(index.specItemCount()).isZero();
    }
}
