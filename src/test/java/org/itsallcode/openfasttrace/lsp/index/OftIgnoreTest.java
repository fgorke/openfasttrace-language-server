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
    void testGivenNoIgnoreFileWhenLoadingThenOnlyTheDefaultsExclude() {
        // when
        final OftIgnore ignore = OftIgnore.load(workspace);

        // then
        assertThat(ignore.isExcluded(workspace.resolve("spec.md"))).isFalse();
        assertThat(ignore.isExcluded(workspace.resolve("target/copy.md"))).isTrue();
    }

    // [utest->req~index-on-startup~2]
    @Test
    void testGivenBuildOutputAtAnyDepthWhenCheckingThenItIsExcluded() {
        // given
        final OftIgnore ignore = OftIgnore.load(workspace);

        // when / then
        assertThat(ignore.isExcluded(workspace.resolve("target/classes/spec.md"))).isTrue();
        assertThat(ignore.isExcluded(workspace.resolve("core/target/classes/spec.md"))).isTrue();
        assertThat(ignore.isExcluded(workspace.resolve("app/ui/build/spec.md"))).isTrue();
        assertThat(ignore.isExcluded(workspace.resolve("web/node_modules/pkg/readme.md"))).isTrue();
        assertThat(ignore.isExcluded(workspace.resolve("core/src/spec.md"))).isFalse();
    }

    // [utest->req~index-on-startup~2]
    @Test
    void testGivenHiddenPathsWhenCheckingThenTheyAreExcludedAtAnyDepth() {
        // given
        final OftIgnore ignore = OftIgnore.load(workspace);

        // when / then
        assertThat(ignore.isExcluded(workspace.resolve(".git/config"))).isTrue();
        assertThat(ignore.isExcluded(workspace.resolve("doc/.hidden/spec.md"))).isTrue();
        assertThat(ignore.isExcluded(workspace.resolve("doc/.oftignore"))).isTrue();
        assertThat(ignore.isExcluded(workspace.resolve("doc/spec.md"))).isFalse();
    }

    // [utest->req~index-ignore-file~1]
    @Test
    void testGivenAnIndexWithoutAWorkspaceWhenCheckingThenNothingIsExcluded() {
        // when / then
        assertThat(OftIgnore.none().isExcluded(workspace.resolve("target/copy.md"))).isFalse();
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
