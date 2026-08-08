package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.lsp4j.TypeHierarchyPrepareParams;
import org.eclipse.lsp4j.TypeHierarchySubtypesParams;
import org.eclipse.lsp4j.TypeHierarchySupertypesParams;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftTextDocumentServiceTypeHierarchyTest {

    private static final int REQUIREMENT_LINE = 8;

    @TempDir
    Path workspace;

    private OftTextDocumentService service;
    private Path spec;

    @BeforeEach
    void setUp() throws Exception {
        spec = workspace.resolve("spec.md");
        Files.writeString(spec, String.join("\n",
                "# Feature", "", "`feat~login~1`", "", "Needs: req", "",
                "# Requirement", "", "`req~login~1`", "", "Covers:", "* feat~login~1", ""));
        service = new OftTextDocumentService();
        service.updateIndex(new WorkspaceIndexer().buildIndex(workspace));
    }

    private TypeHierarchyItem prepareOnRequirement() throws Exception {
        final var params = new TypeHierarchyPrepareParams();
        params.setTextDocument(new TextDocumentIdentifier(spec.toUri().toString()));
        params.setPosition(new Position(REQUIREMENT_LINE, 6));
        final List<TypeHierarchyItem> items = service.prepareTypeHierarchy(params).get();
        assertThat(items).hasSize(1);
        return items.get(0);
    }

    // [itest->req~coverage-hierarchy~1]
    @Test
    void testGivenPositionOnRequirementWhenPreparingThroughTheServiceThenTheFeatureIsReturned()
            throws Exception {
        assertThat(prepareOnRequirement().getName()).isEqualTo("[feat] login~1");
    }

    // [itest->req~coverage-hierarchy~1]
    @Test
    void testGivenTheRootWhenAskingTheServiceForSubtypesThenTheRequirementIsReturned()
            throws Exception {
        // given
        final var params = new TypeHierarchySubtypesParams();
        params.setItem(prepareOnRequirement());

        // when
        final List<TypeHierarchyItem> subtypes = service.typeHierarchySubtypes(params).get();

        // then
        assertThat(subtypes).singleElement()
                .extracting(TypeHierarchyItem::getName).isEqualTo("[req] login~1");
    }

    // [itest->req~coverage-hierarchy~1]
    @Test
    void testGivenTheRootWhenAskingTheServiceForSupertypesThenNothingIsAboveIt() throws Exception {
        // given
        final var params = new TypeHierarchySupertypesParams();
        params.setItem(prepareOnRequirement());

        // when / then
        assertThat(service.typeHierarchySupertypes(params).get()).isEmpty();
    }
}
