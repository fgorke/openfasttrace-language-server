package org.itsallcode.openfasttrace.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.lsp.index.LocationConverter;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.symbols.OftSymbolProvider;
import org.tinylog.Logger;

public class OftWorkspaceService implements WorkspaceService {

    // [impl->req~index-refresh-on-save~1]
    private static final long REINDEX_DEBOUNCE_MS = 300;

    private Runnable onFilesChangedCallback = null;
    private volatile OftWorkspaceIndex index = OftWorkspaceIndex.empty();

    void updateIndex(final OftWorkspaceIndex index) {
        this.index = index;
    }

    // [impl->req~workspace-symbol-search~1]
    @Override
    public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>>
            symbol(final WorkspaceSymbolParams params) {
        final String query = params.getQuery();
        Logger.debug("workspace/symbol: query='" + query + "'");
        return CompletableFuture.supplyAsync(() -> Either.forLeft(symbolsMatching(query)));
    }

    List<SymbolInformation> symbolsMatching(final String query) {
        return OftSymbolProvider.findMatching(index.allSpecItems(), query).stream()
                .map(OftWorkspaceService::toSymbolInformation)
                .toList();
    }

    private static SymbolInformation toSymbolInformation(final SpecificationItem item) {
        return new SymbolInformation(
                OftSymbolProvider.nameOf(item),
                OftSymbolProvider.SYMBOL_KIND,
                LocationConverter.toLspLocation(item.getLocation()),
                OftSymbolProvider.detailOf(item));
    }

    private final ScheduledExecutorService debounceExecutor =
            Executors.newSingleThreadScheduledExecutor(OftWorkspaceService::newDaemonThread);
    private ScheduledFuture<?> pendingReindex;

    private static Thread newDaemonThread(final Runnable runnable) {
        final Thread thread = new Thread(runnable, "oft-lsp-watched-files-debounce");
        thread.setDaemon(true);
        return thread;
    }

    void setOnFilesChangedCallback(final Runnable callback) {
        this.onFilesChangedCallback = callback;
    }

    @Override
    public void didChangeConfiguration(final DidChangeConfigurationParams params) {
        Logger.debug("didChangeConfiguration");
    }

    // [impl->req~index-refresh-on-save~1]
    @Override
    public synchronized void didChangeWatchedFiles(final DidChangeWatchedFilesParams params) {
        Logger.debug("didChangeWatchedFiles: " + params.getChanges().size() + " change(s)");
        if (onFilesChangedCallback == null) {
            return;
        }
        if (pendingReindex != null) {
            pendingReindex.cancel(false);
        }
        pendingReindex = debounceExecutor.schedule(
                onFilesChangedCallback, REINDEX_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }
}
