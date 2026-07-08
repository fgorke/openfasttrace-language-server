package org.itsallcode.openfasttrace.lsp;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.tinylog.Logger;

public class OftWorkspaceService implements WorkspaceService {

    // [impl->req~index-refresh-on-save~1]
    private static final long REINDEX_DEBOUNCE_MS = 300;

    private Runnable onFilesChangedCallback = null;

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
