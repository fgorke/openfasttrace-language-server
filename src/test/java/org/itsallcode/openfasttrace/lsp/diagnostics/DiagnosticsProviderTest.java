package org.itsallcode.openfasttrace.lsp.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiagnosticsProviderTest {

    @TempDir
    Path workspace;

    private DiagnosticsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DiagnosticsProvider();
    }

    private OftWorkspaceIndex indexOf(final Path root) {
        return new WorkspaceIndexer().buildIndex(root);
    }

    private List<Diagnostic> diagnose(final Path file, final OftWorkspaceIndex index)
            throws Exception {
        return provider.diagnoseFile(file.toUri().toString(),
                Files.readAllLines(file), index);
    }

    // [itest->req~diagnostic-outdated-version~2]
    @Test
    void testGivenTagWithOutdatedRevisionWhenDiagnosingThenErrorCarriesTheCurrentId()
            throws Exception {
        // given
        Files.writeString(workspace.resolve("spec.md"),
                "# Login\n\n`req~login~3`\n\nNeeds: impl\n");
        final Path source = workspace.resolve("Login.java");
        Files.writeString(source, "// [impl->req~login~1]\n");

        // when
        final List<Diagnostic> diagnostics = diagnose(source, indexOf(workspace));

        // then
        assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.getSeverity()).isEqualTo(DiagnosticSeverity.Error);
            assertThat(diagnostic.getMessage().getLeft()).contains("req~login").contains("3");
            assertThat(diagnostic.getData()).isEqualTo("req~login~3");
        });
    }

    // [itest->req~diagnostic-outdated-version~2]
    @Test
    void testGivenTagAheadOfTheItemWhenDiagnosingThenWarningCarriesTheCurrentIdAsWell()
            throws Exception {
        // given
        Files.writeString(workspace.resolve("spec.md"),
                "# Login\n\n`req~login~2`\n\nNeeds: impl\n");
        final Path source = workspace.resolve("Login.java");
        Files.writeString(source, "// [impl->req~login~7]\n");

        // when
        final List<Diagnostic> diagnostics = diagnose(source, indexOf(workspace));

        // then
        assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.getMessage().getLeft()).contains("req~login").contains("2");
            assertThat(diagnostic.getData()).isEqualTo("req~login~2");
        });
    }

    // [itest->req~diagnostic-trace-defects~3]
    @Test
    void testGivenTagPointingAtMissingItemWhenDiagnosingThenTheTagIsFlagged() throws Exception {
        // given
        Files.writeString(workspace.resolve("spec.md"), "# Login\n\n`req~login~1`\n");
        final Path source = workspace.resolve("Login.java");
        final String line = "// [impl->req~ghost~1]";
        Files.writeString(source, line + "\n");

        // when
        final List<Diagnostic> diagnostics = diagnose(source, indexOf(workspace));

        // then
        assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.getMessage().getLeft()).contains("req~ghost~1")
                    .contains("does not exist");
            assertThat(diagnostic.getRange().getStart().getCharacter())
                    .isEqualTo(line.indexOf("req~ghost~1"));
            assertThat(diagnostic.getRange().getStart().getLine()).isZero();
        });
    }

    // [itest->req~diagnostic-trace-defects~3]
    @Test
    void testGivenItemMissingRequiredCoverageWhenDiagnosingThenTheDefinitionIsFlagged()
            throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, "# Login\n\n`req~login~1`\n\nNeeds: impl, utest\n");

        // when
        final List<Diagnostic> diagnostics = diagnose(spec, indexOf(workspace));

        // then
        assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.getMessage().getLeft()).contains("impl").contains("utest");
            assertThat(diagnostic.getRange().getStart().getLine()).isEqualTo(2);
        });
    }

    // [itest->req~diagnostic-trace-defects~3]
    @Test
    void testGivenFullyCoveredWorkspaceWhenDiagnosingThenNothingIsReported() throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, "# Login\n\n`req~login~1`\n\nNeeds: impl\n");
        final Path source = workspace.resolve("Login.java");
        Files.writeString(source, "// [impl->req~login~1]\n");
        final OftWorkspaceIndex index = indexOf(workspace);

        // when / then
        assertThat(diagnose(spec, index)).isEmpty();
        assertThat(diagnose(source, index)).isEmpty();
    }

    // [itest->req~diagnostic-trace-defects~3]
    @Test
    void testGivenDefectsInAnotherFileWhenDiagnosingThenOnlyLocalOnesAreReported()
            throws Exception {
        // given
        Files.writeString(workspace.resolve("spec.md"), "# Login\n\n`req~login~1`\n\nNeeds: impl\n");
        Files.writeString(workspace.resolve("Broken.java"), "// [impl->req~ghost~1]\n");
        final Path clean = workspace.resolve("Login.java");
        Files.writeString(clean, "// [impl->req~login~1]\n");

        // when
        final List<Diagnostic> diagnostics = diagnose(clean, indexOf(workspace));

        // then
        assertThat(diagnostics).isEmpty();
    }
}
