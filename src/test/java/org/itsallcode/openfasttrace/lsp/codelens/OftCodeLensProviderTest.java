package org.itsallcode.openfasttrace.lsp.codelens;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lsp4j.CodeLens;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftCodeLensProviderTest {

    @TempDir
    Path workspace;

    private OftWorkspaceIndex indexOf(final String spec, final String source) throws Exception {
        Files.writeString(workspace.resolve("spec.md"), spec);
        if (source != null) {
            Files.writeString(workspace.resolve("Login.java"), source);
        }
        return new WorkspaceIndexer().buildIndex(workspace);
    }

    private List<CodeLens> lensesForSpec(final String spec, final String source) throws Exception {
        final OftWorkspaceIndex index = indexOf(spec, source);
        return OftCodeLensProvider.codeLenses(
                index.linkedItemsInFile(workspace.resolve("spec.md").toUri().toString()));
    }

    private static List<String> titlesOf(final List<CodeLens> lenses) {
        return lenses.stream().map(lens -> lens.getCommand().getTitle()).toList();
    }

    // [itest->req~coverage-code-lens~1]
    @Test
    void testGivenItemMissingCoverageWhenBuildingLensesThenTheMissingTypesAreNamed()
            throws Exception {
        // given
        final String spec = "# Login\n\n`req~login~1`\n\nNeeds: impl, utest\n";

        // when / then
        assertThat(titlesOf(lensesForSpec(spec, null)))
                .containsExactly("missing impl, utest");
    }

    // [itest->req~coverage-code-lens~1]
    @Test
    void testGivenFullyCoveredItemWhenBuildingLensesThenTheCoveringTypesAreNamed()
            throws Exception {
        // given
        final String spec = "# Login\n\n`req~login~1`\n\nNeeds: impl\n";
        final String source = "// [impl->req~login~1]\n// [impl->req~login~1]\n";

        // when / then
        assertThat(titlesOf(lensesForSpec(spec, source))).containsExactly("covered by impl");
    }

    // [itest->req~coverage-code-lens~1]
    @Test
    void testGivenPartiallyCoveredItemWhenBuildingLensesThenBothPartsAreShown() throws Exception {
        // given
        final String spec = "# Login\n\n`req~login~1`\n\nNeeds: impl, utest\n";
        final String source = "// [impl->req~login~1]\n";

        // when / then
        assertThat(titlesOf(lensesForSpec(spec, source)))
                .containsExactly("missing utest · covered by impl");
    }

    // [itest->req~coverage-code-lens~1]
    @Test
    void testGivenItemThatNeitherNeedsNorHasCoverageWhenBuildingLensesThenNoneIsCreated()
            throws Exception {
        assertThat(lensesForSpec("# Note\n\n`req~note~1`\n", null)).isEmpty();
    }

    /** A tag is not a specification item, so summarising its coverage makes no sense. */
    // [itest->req~coverage-code-lens~1]
    @Test
    void testGivenSourceFileWithCoverageTagsWhenBuildingLensesThenNoneIsCreated()
            throws Exception {
        // given
        final String spec = "# Login\n\n`req~login~1`\n\nNeeds: impl\n";
        final OftWorkspaceIndex index = indexOf(spec, "// [impl->req~login~1]\n");

        // when / then
        assertThat(OftCodeLensProvider.codeLenses(index.linkedItemsInFile(
                workspace.resolve("Login.java").toUri().toString()))).isEmpty();
    }
}
