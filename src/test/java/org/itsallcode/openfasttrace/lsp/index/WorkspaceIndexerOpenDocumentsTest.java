package org.itsallcode.openfasttrace.lsp.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.itsallcode.openfasttrace.api.core.LinkedSpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceIndexerOpenDocumentsTest {

    private static final String SPEC_ON_DISK = "# Login\n\n`req~login~1`\n\nNeeds: impl\n";

    private static final String SPEC_IN_EDITOR = "# Login\n\n`req~auth~1`\n\nNeeds: impl\n";

    // [itest->req~index-reads-open-documents~1]
    @Test
    void testGivenAnOpenDocumentWhenIndexingThenItsUnsavedTextIsUsed(@TempDir final Path workspace)
            throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, SPEC_ON_DISK);
        final Map<Path, String> openInEditor = Map.of(spec, SPEC_IN_EDITOR);

        // when
        final var index = new WorkspaceIndexer().buildIndex(workspace, openInEditor);

        // then
        assertThat(index.findSpecItem(SpecificationItemId.parseId("req~auth~1"))).isPresent();
        assertThat(index.findSpecItem(SpecificationItemId.parseId("req~login~1"))).isEmpty();
    }

    // [itest->req~index-reads-open-documents~1]
    @Test
    void testGivenNoOpenDocumentWhenIndexingThenTheFileOnDiskIsUsed(@TempDir final Path workspace)
            throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, SPEC_ON_DISK);

        // when
        final var index = new WorkspaceIndexer().buildIndex(workspace, Map.of());

        // then
        assertThat(index.findSpecItem(SpecificationItemId.parseId("req~login~1"))).isPresent();
    }

    // [itest->req~index-reads-open-documents~1]
    @Test
    void testGivenARenameOnlyHalfWrittenToDiskWhenIndexingThenNoDefectIsReported(
            @TempDir final Path workspace) throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, SPEC_ON_DISK);
        final Path source = workspace.resolve("Main.java");
        Files.writeString(source, "// [impl->req~auth~1]\n");
        final Map<Path, String> specIsOpen = Map.of(spec, SPEC_IN_EDITOR);

        // when
        final var withoutBuffer = new WorkspaceIndexer().buildIndex(workspace, Map.of());
        final var withBuffer = new WorkspaceIndexer().buildIndex(workspace, specIsOpen);

        // then
        assertThat(withoutBuffer.allLinkedItems()).anyMatch(LinkedSpecificationItem::isDefect);
        assertThat(withBuffer.allLinkedItems()).noneMatch(LinkedSpecificationItem::isDefect);
    }
}
