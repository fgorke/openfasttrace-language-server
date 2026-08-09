package org.itsallcode.openfasttrace.lsp.hierarchy;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lsp4j.TypeHierarchyItem;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftTypeHierarchyProviderTest {

    @TempDir
    Path workspace;

    private OftWorkspaceIndex index;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(spec(), String.join("\n",
                "# Login Feature", "", "`feat~login~1`", "", "The feature.", "", "Needs: req", "",
                "# Login Requirement", "", "`req~login~1`", "", "The requirement.", "",
                "Covers:", "* feat~login~1", "", "Needs: dsn", "",
                "# Login Design", "", "`dsn~login~1`", "", "The design.", "",
                "Covers:", "* req~login~1", "", "Needs: impl", ""));
        Files.writeString(source(), "// [impl->dsn~login~1]\n");
        index = new WorkspaceIndexer().buildIndex(workspace);
    }

    private Path spec() {
        return workspace.resolve("spec.md");
    }

    private Path source() {
        return workspace.resolve("Login.java");
    }

    private List<TypeHierarchyItem> prepare(final String line, final int col) {
        return OftTypeHierarchyProvider.prepareAt(line, col, index);
    }

    private TypeHierarchyItem theFeature() {
        final List<TypeHierarchyItem> roots = prepare("`req~login~1`", 6);
        assertThat(roots).hasSize(1);
        return roots.get(0);
    }

    private TypeHierarchyItem onlySubtypeOf(final TypeHierarchyItem item) {
        final List<TypeHierarchyItem> subtypes = OftTypeHierarchyProvider.subtypesOf(item, index);
        assertThat(subtypes).hasSize(1);
        return subtypes.get(0);
    }

    // [itest->req~coverage-hierarchy~2]
    @Test
    void testGivenCursorOnRequirementWhenPreparingThenTheChainStartsAtTheFeature() {
        assertThat(prepare("`req~login~1`", 6)).singleElement().satisfies(root -> {
            assertThat(root.getName()).isEqualTo("[feat] login~1");
            assertThat(root.getDetail()).isEqualTo("spec.md");
        });
    }

    // [itest->req~coverage-hierarchy~2]
    @Test
    void testGivenCursorOnTheFeatureItselfWhenPreparingThenItIsTheRoot() {
        assertThat(prepare("`feat~login~1`", 7)).singleElement()
                .extracting(TypeHierarchyItem::getName).isEqualTo("[feat] login~1");
    }

    // [itest->req~coverage-hierarchy~2]
    @Test
    void testGivenTheFeatureWhenWalkingDownThenTheWholeChainIsReachable() {
        // when
        final TypeHierarchyItem requirement = onlySubtypeOf(theFeature());
        final TypeHierarchyItem design = onlySubtypeOf(requirement);
        final TypeHierarchyItem tag = onlySubtypeOf(design);

        // then
        assertThat(requirement.getName()).isEqualTo("[req] login~1");
        assertThat(design.getName()).isEqualTo("[dsn] login~1");
        assertThat(tag.getName()).isEqualTo("[impl] Login.java:1");
        assertThat(tag.getUri()).endsWith("Login.java");
    }

    // [itest->req~coverage-hierarchy~2]
    @Test
    void testGivenACoverageTagInTheTreeWhenAskingForSupertypesThenTheCoveredItemIsReturned() {
        // given
        final TypeHierarchyItem tag = onlySubtypeOf(onlySubtypeOf(onlySubtypeOf(theFeature())));

        // when / then
        assertThat(OftTypeHierarchyProvider.supertypesOf(tag, index)).singleElement()
                .extracting(TypeHierarchyItem::getName).isEqualTo("[dsn] login~1");
    }

    // [itest->req~coverage-hierarchy~2]
    @Test
    void testGivenItemsCoveringEachOtherWhenPreparingThenTheWalkUpTerminates() throws Exception {
        // given
        Files.writeString(spec(), String.join("\n",
                "# One", "", "`req~one~1`", "", "Covers:", "* req~two~1", "",
                "# Two", "", "`req~two~1`", "", "Covers:", "* req~one~1", ""));
        Files.delete(source());
        index = new WorkspaceIndexer().buildIndex(workspace);

        // when / then
        assertThat(prepare("`req~one~1`", 6)).singleElement()
                .extracting(TypeHierarchyItem::getName).isEqualTo("[req] one~1");
    }
}
