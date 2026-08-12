package org.itsallcode.openfasttrace.lsp.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TraceDiagnosticsTest {

    @TempDir
    Path workspace;

    private List<Diagnostic> diagnose(final Path file) throws Exception {
        final OftWorkspaceIndex index = new WorkspaceIndexer().buildIndex(workspace);
        return TraceDiagnostics.diagnose(index.defectsInFile(file.toUri().toString()),
                Files.readAllLines(file));
    }

    // [itest->req~diagnostic-trace-defects~2]
    @Test
    void testGivenOutdatedTagWhenDiagnosingThenTheOrphanedLinkIsNotReportedAsWell()
            throws Exception {
        // given
        Files.writeString(workspace.resolve("spec.md"),
                "# Login\n\n`req~login~7`\n\nNeeds: impl\n");
        final Path source = workspace.resolve("Login.java");
        Files.writeString(source, "// [impl->req~login~2]\n");

        // when
        final List<Diagnostic> diagnostics = diagnose(source);

        // then
        assertThat(diagnostics).singleElement()
                .extracting(diagnostic -> diagnostic.getMessage().getLeft())
                .asString().contains("Outdated");
    }

    // [itest->req~diagnostic-trace-defects~2]
    @Test
    void testGivenBrokenChainWhenDiagnosingThenOnlyTheCausingItemIsAWarning() throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, String.join("\n",
                "# Feature", "", "`feat~f~1`", "", "Needs: req", "",
                "# Requirement", "", "`req~r~1`", "", "Covers:", "* feat~f~1", "",
                "Needs: dsn", "",
                "# Design", "", "`dsn~d~1`", "", "Covers:", "* req~r~1", "",
                "Needs: impl", ""));

        // when
        final List<Diagnostic> diagnostics = diagnose(spec);

        // then
        assertThat(diagnostics).hasSize(3);
        assertThat(diagnostics).filteredOn(
                diagnostic -> diagnostic.getSeverity() == DiagnosticSeverity.Warning)
                .singleElement()
                .extracting(diagnostic -> diagnostic.getMessage().getLeft())
                .asString().contains("impl");
        assertThat(diagnostics).filteredOn(
                diagnostic -> diagnostic.getSeverity() == DiagnosticSeverity.Information)
                .hasSize(2);
    }

    // [itest->req~diagnostic-trace-defects~2]
    @Test
    void testGivenDuplicateDefinitionsWhenDiagnosingThenBothAreFlagged() throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, String.join("\n",
                "# One", "", "`req~dup~1`", "", "Needs: impl", "",
                "# Two", "", "`req~dup~1`", "", "Needs: impl", ""));

        // when
        final List<Diagnostic> diagnostics = diagnose(spec);

        // then
        assertThat(diagnostics)
                .filteredOn(diagnostic -> diagnostic.getMessage().getLeft().contains("more than once"))
                .hasSize(2);
    }

    // [itest->req~diagnostic-trace-defects~2]
    @Test
    void testGivenDefectOnALineNotPresentInTheBufferWhenDiagnosingThenNothingIsEmitted()
            throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, "# Login\n\n`req~login~1`\n\nNeeds: impl\n");
        final OftWorkspaceIndex index = new WorkspaceIndexer().buildIndex(workspace);

        // when
        final List<Diagnostic> diagnostics =
                TraceDiagnostics.diagnose(index.defectsInFile(spec.toUri().toString()), List.of());

        // then
        assertThat(diagnostics).isEmpty();
    }
}
