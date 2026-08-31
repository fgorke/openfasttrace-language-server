package org.itsallcode.openfasttrace.lsp;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeLensOptions;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.DidChangeWatchedFilesRegistrationOptions;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.FileSystemWatcher;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.RenameOptions;
import org.eclipse.lsp4j.SaveOptions;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.itsallcode.openfasttrace.lsp.highlighting.OftSemanticTokensProvider;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.tinylog.Logger;

public class OftLanguageServer implements LanguageServer, LanguageClientAware {

    private final WorkspaceIndexer indexer;
    private final OftTextDocumentService textDocumentService;
    private final OftWorkspaceService workspaceService;

    private static final String WATCH_WORKSPACE_ID = "oft-watch-workspace";

    private String rootUri;
    private volatile boolean shutdownReceived = false;
    private volatile boolean supportsProgress = false;
    private volatile boolean supportsFileWatchers = false;
    private LanguageClient client;

    public OftLanguageServer() {
        this(new WorkspaceIndexer());
    }

    OftLanguageServer(final WorkspaceIndexer indexer) {
        this.indexer = indexer;
        this.textDocumentService = new OftTextDocumentService();
        this.workspaceService = new OftWorkspaceService();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(final InitializeParams params) {
        this.rootUri = resolveRootUri(params);
        this.supportsProgress = announcesProgressSupport(params);
        this.supportsFileWatchers = announcesFileWatcherSupport(params);
        Logger.info("initialize: rootUri=" + rootUri);

        final var syncOptions = new TextDocumentSyncOptions();
        syncOptions.setOpenClose(true);
        // [impl->req~live-document-buffer~1]
        syncOptions.setChange(TextDocumentSyncKind.Full);
        syncOptions.setSave(new SaveOptions(false));

        final var capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(syncOptions);
        capabilities.setHoverProvider(true);
        capabilities.setDefinitionProvider(true);
        capabilities.setReferencesProvider(true);
        capabilities.setCodeActionProvider(true);
        capabilities.setWorkspaceSymbolProvider(true);
        capabilities.setRenameProvider(new RenameOptions(true));
        capabilities.setTypeHierarchyProvider(true);
        capabilities.setCodeLensProvider(new CodeLensOptions(false));
        capabilities.setExecuteCommandProvider(new ExecuteCommandOptions(
                List.of(OftWorkspaceService.GENERATE_TRACE_REPORT_COMMAND)));
        capabilities.setCompletionProvider(new CompletionOptions(false, List.of("~", ">")));
        capabilities.setSemanticTokensProvider(new SemanticTokensWithRegistrationOptions(
                new SemanticTokensLegend(OftSemanticTokensProvider.TOKEN_TYPES, List.of()), true));

        final var serverInfo = new ServerInfo("OpenFastTrace Language Server", "0.7.0");
        return CompletableFuture.completedFuture(new InitializeResult(capabilities, serverInfo));
    }

    private static boolean announcesProgressSupport(final InitializeParams params) {
        return params.getCapabilities() != null
                && params.getCapabilities().getWindow() != null
                && Boolean.TRUE.equals(params.getCapabilities().getWindow().getWorkDoneProgress());
    }

    private static boolean announcesFileWatcherSupport(final InitializeParams params) {
        return params.getCapabilities() != null
                && params.getCapabilities().getWorkspace() != null
                && params.getCapabilities().getWorkspace().getDidChangeWatchedFiles() != null
                && Boolean.TRUE.equals(params.getCapabilities().getWorkspace()
                        .getDidChangeWatchedFiles().getDynamicRegistration());
    }

    private static String resolveRootUri(final InitializeParams params) {
        final List<WorkspaceFolder> folders = params.getWorkspaceFolders();
        if (folders != null && !folders.isEmpty()) {
            if (folders.size() > 1) {
                Logger.warn("Multiple workspace folders received, indexing only the first: "
                        + folders.get(0).getUri());
            }
            return folders.get(0).getUri();
        }
        return params.getRootUri();
    }

    // [impl->req~index-on-startup~3]
    @Override
    public void initialized(final InitializedParams params) {
        Logger.info("initialized, building workspace index");
        if (rootUri == null) {
            Logger.warn("No rootUri available, skipping index");
            return;
        }
        textDocumentService.setOnSaveCallback(this::rebuildIndex);
        workspaceService.setOnFilesChangedCallback(this::rebuildIndex);
        watchWorkspaceFiles();
        CompletableFuture.runAsync(this::buildInitialIndex);
    }

    // [impl->req~index-refresh-on-file-change~1]
    private void watchWorkspaceFiles() {
        if (client == null || !supportsFileWatchers) {
            Logger.info("Client does not watch files, a change outside the editor"
                    + " takes effect on the next save");
            return;
        }
        final var watcher = new FileSystemWatcher(Either.forLeft("**/*"));
        final var registration = new Registration(WATCH_WORKSPACE_ID,
                "workspace/didChangeWatchedFiles",
                new DidChangeWatchedFilesRegistrationOptions(List.of(watcher)));
        client.registerCapability(new RegistrationParams(List.of(registration)))
                .exceptionally(exception -> {
                    Logger.warn("Could not register a workspace file watcher: "
                            + exception.getMessage());
                    return null;
                });
    }

    private void buildInitialIndex() {
        final ProgressReport progress = ProgressReport.start(client, supportsProgress,
                "OFT: indexing the workspace");
        try {
            rebuildIndex();
        } finally {
            progress.finish();
        }
    }

    private void rebuildIndex() {
        if (rootUri == null) {
            return;
        }
        final Path workspaceRoot = Path.of(URI.create(rootUri));
        final OftWorkspaceIndex index = indexer.buildIndex(workspaceRoot);
        textDocumentService.updateIndex(index);
        workspaceService.updateIndex(index);
        Logger.info("Workspace index ready: " + index.specItemCount() + " item(s)");
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        Logger.info("shutdown requested");
        shutdownReceived = true;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        Logger.info("exit");
        System.exit(shutdownReceived ? 0 : 1);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(final LanguageClient client) {
        Logger.info("client connected");
        this.client = client;
        textDocumentService.connect(client);
    }
}
