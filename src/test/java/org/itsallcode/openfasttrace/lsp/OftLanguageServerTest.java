package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftLanguageServerTest {

    private OftLanguageServer server;

    @BeforeEach
    void setUp() {
        server = new OftLanguageServer();
    }

    // [utest->req~index-on-startup~1]
    @Test
    void testGivenWorkspaceFoldersAndRootUriWhenInitializingThenIndexIsBuiltFromWorkspaceFolder(
            @TempDir final Path fromFolders, @TempDir final Path fromRootUri) throws Exception {
        // given
        final var indexer = mock(WorkspaceIndexer.class);
        when(indexer.buildIndex(any())).thenReturn(OftWorkspaceIndex.empty());
        final var serverWithIndexer = new OftLanguageServer(indexer);
        final var params = new InitializeParams();
        params.setWorkspaceFolders(
                List.of(new WorkspaceFolder(fromFolders.toUri().toString(), "workspace")));
        setDeprecatedRootUri(params, fromRootUri);

        // when
        serverWithIndexer.initialize(params).get();
        serverWithIndexer.initialized(new InitializedParams());

        // then
        verify(indexer).buildIndex(fromFolders);
    }

    // [utest->req~index-on-startup~1]
    @Test
    void testGivenOnlyRootUriWhenInitializingThenIndexIsBuiltFromRootUri(@TempDir final Path root)
            throws Exception {
        // given
        final var indexer = mock(WorkspaceIndexer.class);
        when(indexer.buildIndex(any())).thenReturn(OftWorkspaceIndex.empty());
        final var serverWithIndexer = new OftLanguageServer(indexer);
        final var params = new InitializeParams();
        setDeprecatedRootUri(params, root);

        // when
        serverWithIndexer.initialize(params).get();
        serverWithIndexer.initialized(new InitializedParams());

        // then
        verify(indexer).buildIndex(root);
    }

    private static void setDeprecatedRootUri(final InitializeParams params, final Path root) {
        params.setRootUri(root.toUri().toString());
    }

    // [utest->req~complete-specification-item-id-in-coverage-tag-target~1]
    @Test
    void testGivenInitializeParamsWhenInitializingThenHyphenIsNotATriggerCharacter()
            throws Exception {
        // given
        final var params = new InitializeParams();

        // when
        final InitializeResult result = server.initialize(params).get();

        // then
        final var triggerCharacters =
                result.getCapabilities().getCompletionProvider().getTriggerCharacters();
        assertThat(triggerCharacters).contains("~", ">").doesNotContain("-", "[");
    }

    @Test
    void testGivenInitializeParamsWhenInitializingThenTextDocumentSyncIsDeclared() throws Exception {
        // given
        final var params = new InitializeParams();

        // when
        final InitializeResult result = server.initialize(params).get();

        // then
        final var sync = result.getCapabilities().getTextDocumentSync().getRight();
        assertThat(sync).isNotNull();
        assertThat(sync).isInstanceOf(TextDocumentSyncOptions.class);
        assertThat(sync.getOpenClose()).isTrue();
        assertThat(sync.getSave()).isNotNull();
    }
}
