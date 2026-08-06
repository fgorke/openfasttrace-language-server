package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.itsallcode.openfasttrace.api.core.Location;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftTextDocumentServiceRenameTest {

    @TempDir
    Path workspace;

    private OftTextDocumentService service;

    @BeforeEach
    void setUp() {
        service = new OftTextDocumentService();
    }

    private static SpecificationItem itemAt(final String id, final Path file, final int line) {
        return SpecificationItem.builder()
                .id(SpecificationItemId.parseId(id))
                .title("Title")
                .description("")
                .location(Location.create(file.toString(), line))
                .build();
    }

    // [itest->req~rename-specification-item~1]
    @Test
    void testGivenItemAndCoveringTagWhenRenamingThenBothFilesAreEdited() throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, "# Login\n\n`req~login~1`\n\nNeeds: impl\n");
        final Path source = workspace.resolve("Main.java");
        Files.writeString(source, "class Main {\n    // [impl->req~login~1]\n}\n");
        service.updateIndex(new OftWorkspaceIndex(List.of(
                itemAt("req~login~1", spec, 3),
                itemAt("impl~login-1~0", source, 2))));

        // when
        final WorkspaceEdit edit =
                service.renameEdits(spec.toUri().toString(), 2, 6, "authentication");

        // then
        final Map<String, List<TextEdit>> changes = edit.getChanges();
        assertThat(changes).hasSize(2);
        assertThat(changes.get(spec.toUri().toString())).singleElement()
                .extracting(TextEdit::getNewText).isEqualTo("authentication");
        assertThat(changes.get(source.toUri().toString())).singleElement()
                .extracting(TextEdit::getNewText).isEqualTo("authentication");
    }

    // [itest->req~rename-specification-item~1]
    @Test
    void testGivenCursorOnTagWhenRenamingThenTheDefinitionIsUpdatedAsWell() throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, "# Login\n\n`req~login~1`\n");
        final Path source = workspace.resolve("Main.java");
        Files.writeString(source, "// [impl->req~login~1]\n");
        service.updateIndex(new OftWorkspaceIndex(List.of(
                itemAt("req~login~1", spec, 3),
                itemAt("impl~login-1~0", source, 1))));

        // when
        final String tagLine = Files.readString(source).strip();
        final WorkspaceEdit edit = service.renameEdits(source.toUri().toString(), 0,
                tagLine.indexOf("login", tagLine.indexOf("->")), "authentication");

        // then
        assertThat(edit.getChanges()).hasSize(2);
    }

    // [itest->req~rename-specification-item~1]
    @Test
    void testGivenCoversEntryInAnotherSpecFileWhenRenamingThenItIsUpdated() throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, "# Login\n\n`feat~login~1`\n");
        final Path requirements = workspace.resolve("requirements.md");
        Files.writeString(requirements,
                "# Detail\n\n`req~detail~1`\n\nCovers:\n* feat~login~1\n\nNeeds: impl\n");
        service.updateIndex(new OftWorkspaceIndex(List.of(
                itemAt("feat~login~1", spec, 3),
                itemAt("req~detail~1", requirements, 3))));

        // when
        final WorkspaceEdit edit = service.renameEdits(spec.toUri().toString(), 2, 7, "auth");

        // then
        final List<TextEdit> edits = edit.getChanges().get(requirements.toUri().toString());
        assertThat(edits).singleElement().extracting(e -> e.getRange().getStart().getLine())
                .isEqualTo(5);
    }

    // [utest->req~rename-name-part-only~1]
    @Test
    void testGivenInvalidNewNameWhenRenamingThenTheRequestIsRejected() throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, "`req~login~1`\n");
        service.updateIndex(new OftWorkspaceIndex(List.of(itemAt("req~login~1", spec, 1))));

        // when / then
        assertThatThrownBy(() -> service.renameEdits(spec.toUri().toString(), 0, 6, "not a name"))
                .isInstanceOf(ResponseErrorException.class)
                .hasMessageContaining("not a valid specification item name");
    }

    // [utest->req~rename-conflict-check~1]
    @Test
    void testGivenTargetNameAlreadyUsedByAnotherItemWhenRenamingThenTheRequestIsRejected()
            throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, "`req~login~1`\n\n`req~taken~1`\n");
        service.updateIndex(new OftWorkspaceIndex(List.of(
                itemAt("req~login~1", spec, 1),
                itemAt("req~taken~1", spec, 3))));

        // when / then
        assertThatThrownBy(() -> service.renameEdits(spec.toUri().toString(), 0, 6, "taken"))
                .isInstanceOf(ResponseErrorException.class)
                .hasMessageContaining("already exists");
    }

    // [utest->req~rename-specification-item~1]
    @Test
    void testGivenCursorNotOnAnIdWhenRenamingThenTheRequestIsRejected() throws Exception {
        // given
        final Path spec = workspace.resolve("spec.md");
        Files.writeString(spec, "just prose\n");
        service.updateIndex(OftWorkspaceIndex.empty());

        // when / then
        assertThatThrownBy(() -> service.renameEdits(spec.toUri().toString(), 0, 3, "auth"))
                .isInstanceOf(ResponseErrorException.class)
                .hasMessageContaining("no specification item ID at the cursor");
    }

    // [utest->req~prepare-rename~1]
    @Test
    void testGivenCursorOutsideAnIdWhenPreparingRenameThenRenameIsUnavailable() {
        assertThat(service.prepareRenameAt("just prose", 0, 3)).isEmpty();
    }
}
