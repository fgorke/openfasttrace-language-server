package org.itsallcode.openfasttrace.lsp.index;

import java.nio.file.Path;
import java.util.List;

import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.importer.ImportSettings;
import org.itsallcode.openfasttrace.core.OftRunner;
import org.tinylog.Logger;

// [impl->req~index-on-startup~1]
// [impl->req~index-refresh-on-save~1]
public class WorkspaceIndexer {

    private final OftRunner runner;

    public WorkspaceIndexer() {
        this(new OftRunner());
    }

    WorkspaceIndexer(final OftRunner runner) {
        this.runner = runner;
    }

    public OftWorkspaceIndex buildIndex(final Path workspaceRoot) {
        Logger.info("Indexing workspace: " + workspaceRoot);
        final ImportSettings settings = ImportSettings.builder()
                .addInputs(workspaceRoot)
                .build();
        final List<SpecificationItem> items = runner.importItems(settings);
        Logger.info("Indexed " + items.size() + " specification item(s)");
        return new OftWorkspaceIndex(items);
    }
}
