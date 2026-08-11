package org.itsallcode.openfasttrace.lsp.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftIgnoreTest {

    @TempDir
    Path workspace;

    private OftIgnore ignoreWith(final String content) throws Exception {
        Files.writeString(workspace.resolve(OftIgnore.FILE_NAME), content);
        return OftIgnore.load(workspace);
    }

    // [utest->req~index-ignore-file~1]
    @Test
    void testGivenNoIgnoreFileWhenLoadingThenNothingIsExcluded() {
        // when
        final OftIgnore ignore = OftIgnore.load(workspace);

        // then
        assertThat(ignore.isExcluded(workspace.resolve("spec.md"))).isFalse();
    }

    // [utest->req~index-ignore-file~1]
    @Test
    void testGivenDirectoryPatternWhenCheckingAFileBelowItThenItIsExcluded() throws Exception {
        // given
        final OftIgnore ignore = ignoreWith("doc/demo\n");

        // when / then
        assertThat(ignore.isExcluded(workspace.resolve("doc/demo/example/spec.md"))).isTrue();
        assertThat(ignore.isExcluded(workspace.resolve("doc/spec/requirements.md"))).isFalse();
    }

    // [utest->req~index-ignore-file~1]
    @Test
    void testGivenGlobPatternWhenCheckingMatchingFilesThenTheyAreExcluded() throws Exception {
        // given
        final OftIgnore ignore = ignoreWith("**/*.gen.md\n");

        // when / then
        assertThat(ignore.isExcluded(workspace.resolve("doc/report.gen.md"))).isTrue();
        assertThat(ignore.isExcluded(workspace.resolve("doc/report.md"))).isFalse();
    }

    // [utest->req~index-ignore-file~1]
    @Test
    void testGivenCommentsAndBlankLinesWhenLoadingThenTheyAreSkipped() throws Exception {
        // given
        final OftIgnore ignore = ignoreWith("# a comment\n\ndocs/\n");

        // when / then
        assertThat(ignore.isExcluded(workspace.resolve("docs/readme.md"))).isTrue();
        assertThat(ignore.isExcluded(workspace.resolve("# a comment"))).isFalse();
    }

    // [utest->req~index-ignore-file~1]
    @Test
    void testGivenAFileUriWhenCheckingThenItIsResolvedLikeAPath() throws Exception {
        // given
        final OftIgnore ignore = ignoreWith("demo\n");

        // when / then
        assertThat(ignore.isExcluded(workspace.resolve("demo/spec.md").toUri().toString()))
                .isTrue();
    }

    // [utest->req~index-ignore-file~1]
    @Test
    void testGivenAPathOutsideTheWorkspaceWhenCheckingThenItIsNotExcluded() throws Exception {
        // given
        final OftIgnore ignore = ignoreWith("**\n");

        // when / then
        assertThat(ignore.isExcluded(workspace.getParent().resolve("outside.md"))).isFalse();
    }
}
