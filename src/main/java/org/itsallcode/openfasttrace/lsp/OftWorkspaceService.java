package org.itsallcode.openfasttrace.lsp;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.google.gson.JsonPrimitive;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.lsp.index.LocationConverter;
import org.itsallcode.openfasttrace.lsp.index.OftIgnore;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.report.TraceReportGenerator;
import org.itsallcode.openfasttrace.lsp.report.TraceReportPreset;
import org.itsallcode.openfasttrace.lsp.symbols.OftSymbolProvider;
import org.tinylog.Logger;

public class OftWorkspaceService implements WorkspaceService {

    // [impl->req~index-refresh-on-file-change~1]
    private static final long REINDEX_DEBOUNCE_MS = 300;

    private Runnable onFilesChangedCallback = null;
    private volatile OftWorkspaceIndex index = OftWorkspaceIndex.empty();

    void updateIndex(final OftWorkspaceIndex index) {
        this.index = index;
    }

    public static final String GENERATE_TRACE_REPORT_COMMAND = "oft.generateTraceReport";

    private final TraceReportGenerator reportGenerator = new TraceReportGenerator();

    // [impl->req~trace-report-on-request~1]
    @Override
    public CompletableFuture<Object> executeCommand(final ExecuteCommandParams params) {
        if (!GENERATE_TRACE_REPORT_COMMAND.equals(params.getCommand())) {
            Logger.warn("Unknown command: " + params.getCommand());
            return CompletableFuture.completedFuture(null);
        }
        final TraceReportPreset preset = presetFrom(params.getArguments());
        return CompletableFuture.supplyAsync(() -> {
            try {
                return reportGenerator.generate(index.allLinkedItems(), preset).toString();
            } catch (final IOException exception) {
                Logger.error("Could not write the trace report: " + exception.getMessage());
                throw new CompletionException(exception);
            }
        });
    }

    private static TraceReportPreset presetFrom(final List<Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return TraceReportPreset.HTML;
        }
        final Object first = arguments.get(0);
        final String id = first instanceof JsonPrimitive primitive ? primitive.getAsString()
                : String.valueOf(first);
        return TraceReportPreset.byId(id).orElseGet(() -> {
            Logger.warn("Unknown report preset '" + id + "', falling back to "
                    + TraceReportPreset.HTML.id());
            return TraceReportPreset.HTML;
        });
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

    // [impl->req~index-refresh-on-file-change~1]
    @Override
    public synchronized void didChangeWatchedFiles(final DidChangeWatchedFilesParams params) {
        Logger.debug("didChangeWatchedFiles: " + params.getChanges().size() + " change(s)");
        if (onFilesChangedCallback == null || !touchesIndexedFile(params)) {
            return;
        }
        if (pendingReindex != null) {
            pendingReindex.cancel(false);
        }
        pendingReindex = debounceExecutor.schedule(
                onFilesChangedCallback, REINDEX_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    // [impl->req~index-refresh-on-file-change~1]
    private boolean touchesIndexedFile(final DidChangeWatchedFilesParams params) {
        return params.getChanges().stream()
                .map(FileEvent::getUri)
                .filter(Objects::nonNull)
                .anyMatch(uri -> isIgnoreFile(uri) || index.isIndexedFile(uri));
    }

    private static boolean isIgnoreFile(final String uri) {
        return LocationConverter.toPath(uri)
                .map(path -> OftIgnore.FILE_NAME.equals(String.valueOf(path.getFileName())))
                .orElse(false);
    }
}
