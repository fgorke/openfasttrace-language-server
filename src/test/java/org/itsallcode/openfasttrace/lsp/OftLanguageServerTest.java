package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DidChangeWatchedFilesCapabilities;
import org.eclipse.lsp4j.DidChangeWatchedFilesRegistrationOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.mockito.ArgumentCaptor;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftLanguageServerTest {

    private static final int INDEX_TIMEOUT_MS = 5_000;

    private OftLanguageServer server;

    @BeforeEach
    void setUp() {
        server = new OftLanguageServer();
    }

    // [utest->req~index-on-startup~3]
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
        verify(indexer, timeout(INDEX_TIMEOUT_MS)).buildIndex(fromFolders);
    }

    // [utest->req~index-on-startup~3]
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
        verify(indexer, timeout(INDEX_TIMEOUT_MS)).buildIndex(root);
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

    // [itest->req~coverage-hierarchy~2]
    @Test
    void testGivenInitializeParamsWhenInitializingThenTypeHierarchyIsDeclared() throws Exception {
        // given
        final var params = new InitializeParams();

        // when
        final InitializeResult result = server.initialize(params).get();

        // then
        assertThat(result.getCapabilities().getTypeHierarchyProvider().getLeft()).isTrue();
    }

    // [utest->req~index-refresh-on-file-change~1]
    @Test
    void testGivenAClientThatWatchesFilesWhenInitializedThenTheWholeWorkspaceIsWatched(
            @TempDir final Path root) throws Exception {
        // given
        final var client = mock(LanguageClient.class);
        when(client.registerCapability(any())).thenReturn(CompletableFuture.completedFuture(null));
        final var serverWithClient = startedServer(client, root, true);

        // when
        serverWithClient.initialized(new InitializedParams());

        // then
        final var captor = ArgumentCaptor.forClass(RegistrationParams.class);
        verify(client).registerCapability(captor.capture());
        final Registration registration = captor.getValue().getRegistrations().get(0);
        assertThat(registration.getMethod()).isEqualTo("workspace/didChangeWatchedFiles");
        final var options = (DidChangeWatchedFilesRegistrationOptions) registration.getRegisterOptions();
        assertThat(options.getWatchers()).singleElement()
                .extracting(watcher -> watcher.getGlobPattern().getLeft())
                .isEqualTo("**/*");
    }

    private static OftLanguageServer startedServer(final LanguageClient client, final Path root,
            final boolean watchesFiles) throws Exception {
        final var indexer = mock(WorkspaceIndexer.class);
        when(indexer.buildIndex(any())).thenReturn(OftWorkspaceIndex.empty());
        final var server = new OftLanguageServer(indexer);
        server.connect(client);
        final var params = new InitializeParams();
        params.setRootUri(root.toUri().toString());
        if (watchesFiles) {
            final var watchedFiles = new DidChangeWatchedFilesCapabilities();
            watchedFiles.setDynamicRegistration(true);
            final var workspace = new WorkspaceClientCapabilities();
            workspace.setDidChangeWatchedFiles(watchedFiles);
            final var capabilities = new ClientCapabilities();
            capabilities.setWorkspace(workspace);
            params.setCapabilities(capabilities);
        }
        server.initialize(params).get();
        return server;
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
