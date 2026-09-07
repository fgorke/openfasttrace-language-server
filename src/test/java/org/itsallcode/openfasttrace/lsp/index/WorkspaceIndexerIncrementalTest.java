package org.itsallcode.openfasttrace.lsp.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.itsallcode.openfasttrace.api.core.LinkedSpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceIndexerIncrementalTest {

    @TempDir
    Path workspace;

    private WorkspaceIndexer indexer;

    @BeforeEach
    void setUp() {
        indexer = new WorkspaceIndexer();
    }

    private static String spec(final String name) {
        return "# Title\n\n`req~" + name + "~1`\n\nA requirement.\n";
    }

    private Path write(final String fileName, final String content, final Duration age)
            throws IOException {
        final Path file = workspace.resolve(fileName);
        Files.writeString(file, content);
        Files.setLastModifiedTime(file, FileTime.from(Instant.now().minus(age)));
        return file;
    }

    private OftWorkspaceIndex reindex() {
        return indexer.buildIndex(workspace, Map.of());
    }

    private static boolean holds(final OftWorkspaceIndex index, final String id) {
        return index.findSpecItem(SpecificationItemId.parseId(id)).isPresent();
    }

    // [itest->req~index-imports-changed-files-only~1]
    @Test
    void testGivenAChangedFileWhenReindexingThenItsItemsAreUpdated() throws Exception {
        // given
        write("spec.md", spec("login"), Duration.ofMinutes(5));
        assertThat(holds(reindex(), "req~login~1")).isTrue();

        // when
        write("spec.md", spec("auth"), Duration.ZERO);

        // then
        final OftWorkspaceIndex index = reindex();
        assertThat(holds(index, "req~auth~1")).isTrue();
        assertThat(holds(index, "req~login~1")).isFalse();
    }

    // [itest->req~index-imports-changed-files-only~1]
    @Test
    void testGivenAnUntouchedFileWhenReindexingThenItsItemsSurvive() throws Exception {
        // given
        write("kept.md", spec("login"), Duration.ofMinutes(5));
        write("edited.md", spec("logout"), Duration.ofMinutes(5));
        reindex();

        // when
        write("edited.md", spec("sign-out"), Duration.ZERO);

        // then
        final OftWorkspaceIndex index = reindex();
        assertThat(holds(index, "req~login~1")).isTrue();
        assertThat(holds(index, "req~sign-out~1")).isTrue();
    }

    // [itest->req~index-imports-changed-files-only~1]
    @Test
    void testGivenADeletedFileWhenReindexingThenItsItemsAreGone() throws Exception {
        // given
        final Path spec = write("spec.md", spec("login"), Duration.ofMinutes(5));
        reindex();

        // when
        Files.delete(spec);

        // then
        assertThat(holds(reindex(), "req~login~1")).isFalse();
    }

    // [itest->req~index-imports-changed-files-only~1]
    @Test
    void testGivenAnAddedFileWhenReindexingThenItsItemsAppear() throws Exception {
        // given
        write("spec.md", spec("login"), Duration.ofMinutes(5));
        reindex();

        // when
        write("more.md", spec("logout"), Duration.ZERO);

        // then
        assertThat(holds(reindex(), "req~logout~1")).isTrue();
    }

    // [itest->req~index-imports-changed-files-only~1]
    @Test
    void testGivenAFileTheEditorHoldsWhenReindexingThenItIsImportedAgain() throws Exception {
        // given
        final Path spec = write("spec.md", spec("login"), Duration.ofMinutes(5));
        reindex();

        // when
        final OftWorkspaceIndex index = indexer.buildIndex(workspace, Map.of(spec, spec("auth")));

        // then
        assertThat(holds(index, "req~auth~1")).isTrue();
        assertThat(holds(index, "req~login~1")).isFalse();
    }

    // [itest->req~index-imports-changed-files-only~1]
    @Test
    void testGivenTheEditorClosedTheFileWhenReindexingThenTheFileOnDiskCountsAgain()
            throws Exception {
        // given
        final Path spec = write("spec.md", spec("login"), Duration.ofMinutes(5));
        indexer.buildIndex(workspace, Map.of(spec, spec("auth")));

        // when
        final OftWorkspaceIndex index = reindex();

        // then
        assertThat(holds(index, "req~login~1")).isTrue();
        assertThat(holds(index, "req~auth~1")).isFalse();
    }

    // [itest->req~index-imports-changed-files-only~1]
    @Test
    void testGivenASeriesOfChangesWhenReindexingThenTheResultMatchesAFullBuild() throws Exception {
        // given
        write("a.md", spec("login"), Duration.ofMinutes(5));
        write("b.md", spec("logout"), Duration.ofMinutes(5));
        write("Login.java", "// [impl->req~login~1]\n", Duration.ofMinutes(5));
        reindex();

        // when
        write("b.md", spec("sign-out"), Duration.ZERO);
        write("c.md", spec("reset"), Duration.ZERO);
        Files.delete(workspace.resolve("a.md"));

        // then
        final OftWorkspaceIndex incremental = reindex();
        final OftWorkspaceIndex full = new WorkspaceIndexer().buildIndex(workspace);
        assertThat(incremental.allSpecItems()).isEqualTo(full.allSpecItems());
        assertThat(incremental.allLinkedItems().stream().filter(LinkedSpecificationItem::isDefect).count())
                .isEqualTo(full.allLinkedItems().stream().filter(LinkedSpecificationItem::isDefect).count());
    }
}
