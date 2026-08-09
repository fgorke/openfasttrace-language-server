package org.itsallcode.openfasttrace.lsp.index;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.itsallcode.openfasttrace.api.core.LinkedSpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.importer.ImportSettings;
import org.itsallcode.openfasttrace.core.OftRunner;
import org.tinylog.Logger;

// [impl->req~index-refresh-on-save~1, req~index-on-startup~1]
public class WorkspaceIndexer {

    private static final Set<String> EXCLUDED_DIRECTORY_NAMES =
            Set.of("target", "build", "out", "dist", "node_modules");

    private final OftRunner runner;

    public WorkspaceIndexer() {
        this(new OftRunner());
    }

    WorkspaceIndexer(final OftRunner runner) {
        this.runner = runner;
    }

    // [impl->req~diagnostic-trace-defects~1]
    public OftWorkspaceIndex buildIndex(final Path workspaceRoot) {
        Logger.info("Indexing workspace: " + workspaceRoot);
        final ImportSettings settings = ImportSettings.builder()
                .addInputs(collectInputs(workspaceRoot))
                .build();
        final List<SpecificationItem> items = runner.importItems(settings);
        final List<LinkedSpecificationItem> linkedItems = runner.link(items);
        Logger.info("Indexed " + items.size() + " specification item(s), "
                + linkedItems.stream().filter(LinkedSpecificationItem::isDefect).count()
                + " defect(s)");
        return OftWorkspaceIndex.ofLinkedItems(linkedItems);
    }

    @SuppressWarnings("NullableProblems")
    private static List<Path> collectInputs(final Path workspaceRoot) {
        final List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(workspaceRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                    if (!dir.equals(workspaceRoot) && isExcluded(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                    if (!file.getFileName().toString().startsWith(".")
                            && OftSupportedFiles.isSupported(file)) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(final Path file, final IOException exception) {
                    Logger.debug("Skipping unreadable file: " + file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException exception) {
            Logger.warn("Workspace walk failed, indexing the full root instead: "
                    + exception.getMessage());
            return List.of(workspaceRoot);
        }
        return files;
    }

    private static boolean isExcluded(final String directoryName) {
        return directoryName.startsWith(".") || EXCLUDED_DIRECTORY_NAMES.contains(directoryName);
    }
}
