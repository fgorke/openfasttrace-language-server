package org.itsallcode.openfasttrace.lsp.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.itsallcode.openfasttrace.api.core.LinkedSpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.importer.ImportSettings;
import org.itsallcode.openfasttrace.api.importer.ImporterContext;
import org.itsallcode.openfasttrace.api.importer.ImporterService;
import org.itsallcode.openfasttrace.api.importer.MultiFileImporter;
import org.itsallcode.openfasttrace.api.importer.input.InputFile;
import org.itsallcode.openfasttrace.api.importer.input.RealFileInput;
import org.itsallcode.openfasttrace.core.OftRunner;
import org.itsallcode.openfasttrace.core.importer.ImporterFactoryLoader;
import org.itsallcode.openfasttrace.core.importer.ImporterServiceImpl;
import org.tinylog.Logger;

// [impl->req~index-refresh-on-file-change~1, req~index-on-startup~3]
// [impl->adr~import-the-workspace-on-start-and-re-import-on-save~1]
public class WorkspaceIndexer {

    private final OftRunner runner;

    private final Map<Path, CachedImport> importCache = new ConcurrentHashMap<>();

    public WorkspaceIndexer() {
        this(new OftRunner());
    }

    WorkspaceIndexer(final OftRunner runner) {
        this.runner = runner;
    }

    public OftWorkspaceIndex buildIndex(final Path workspaceRoot) {
        return buildIndex(workspaceRoot, Map.of());
    }

    // [impl->req~diagnostic-trace-defects~3]
    public OftWorkspaceIndex buildIndex(final Path workspaceRoot,
            final Map<Path, String> openDocuments) {
        Logger.info("Indexing workspace: " + workspaceRoot);
        // [impl->req~index-ignore-file~1]
        final OftIgnore ignore = OftIgnore.load(workspaceRoot);
        final List<FileToImport> files = collectInputs(workspaceRoot, ignore);
        final List<SpecificationItem> items = importItems(files, openDocuments);
        final List<LinkedSpecificationItem> linkedItems = runner.link(items);
        Logger.info("Indexed " + items.size() + " specification item(s), "
                + linkedItems.stream().filter(LinkedSpecificationItem::isDefect).count()
                + " defect(s)");
        return OftWorkspaceIndex.ofLinkedItems(linkedItems, ignore);
    }

    private record FileToImport(Path path, String fingerprint) {

        static FileToImport of(final Path path, final BasicFileAttributes attributes) {
            return new FileToImport(path, attributes.size() + "@"
                    + attributes.lastModifiedTime().to(TimeUnit.NANOSECONDS));
        }

        static FileToImport unknown(final Path path) {
            return new FileToImport(path, null);
        }
    }

    private record CachedImport(String fingerprint, List<SpecificationItem> items) {
    }

    // [impl->req~index-imports-changed-files-only~1]
    // [impl->adr~import-only-changed-files-when-rebuilding-the-index~1]
    private List<SpecificationItem> importItems(final List<FileToImport> files,
            final Map<Path, String> openDocuments) {
        importCache.keySet().retainAll(files.stream().map(FileToImport::path).collect(toSet()));
        final List<FileToImport> changed = files.stream()
                .filter(file -> hasChanged(file, openDocuments))
                .toList();
        final Map<Path, List<SpecificationItem>> imported = importChanged(changed, openDocuments);
        changed.forEach(file -> cache(file, imported, openDocuments));
        Logger.debug("Imported " + changed.size() + " of " + files.size() + " file(s)");
        return files.stream()
                .map(file -> imported.containsKey(file.path())
                        ? imported.get(file.path())
                        : cachedItemsOf(file))
                .flatMap(List::stream)
                .toList();
    }

    private boolean hasChanged(final FileToImport file, final Map<Path, String> openDocuments) {
        if (file.fingerprint() == null || openDocuments.containsKey(file.path())) {
            return true;
        }
        final CachedImport cached = importCache.get(file.path());
        return cached == null || !cached.fingerprint().equals(file.fingerprint());
    }

    private void cache(final FileToImport file, final Map<Path, List<SpecificationItem>> imported,
            final Map<Path, String> openDocuments) {
        if (file.fingerprint() == null || openDocuments.containsKey(file.path())) {
            importCache.remove(file.path());
            return;
        }
        importCache.put(file.path(), new CachedImport(file.fingerprint(),
                imported.getOrDefault(file.path(), List.of())));
    }

    private List<SpecificationItem> cachedItemsOf(final FileToImport file) {
        final CachedImport cached = importCache.get(file.path());
        return cached == null ? List.of() : cached.items();
    }

    // [impl->req~index-reads-open-documents~1]
    private static Map<Path, List<SpecificationItem>> importChanged(
            final List<FileToImport> files, final Map<Path, String> openDocuments) {
        if (files.isEmpty()) {
            return Map.of();
        }
        final List<Path> paths = files.stream().map(FileToImport::path).toList();
        final ImportSettings settings = ImportSettings.builder().addInputs(paths).build();
        final ImporterContext context = new ImporterContext(settings);
        final ImporterService importerService =
                new ImporterServiceImpl(new ImporterFactoryLoader(context), settings);
        context.setImporterService(importerService);
        final MultiFileImporter importer = importerService.createImporter();
        for (final Path file : paths) {
            importer.importFile(inputFor(file, openDocuments));
        }
        return importer.getImportedItems().stream()
                .collect(groupingBy(item -> Path.of(item.getLocation().getPath()),
                        LinkedHashMap::new, toList()));
    }

    private static InputFile inputFor(final Path file, final Map<Path, String> openDocuments) {
        final String inEditor = openDocuments.get(file);
        return inEditor == null ? RealFileInput.forPath(file)
                : new OpenDocumentInput(file, inEditor);
    }

    private record OpenDocumentInput(Path path, String content) implements InputFile {
        @Override
        public BufferedReader createReader() {
            return new BufferedReader(new StringReader(content));
        }

        @Override
        public String getPath() {
            return path.toString();
        }

        @Override
        public boolean isRealFile() {
            return true;
        }

        @Override
        public Path toPath() {
            return path;
        }
    }

    @SuppressWarnings("NullableProblems")
    private static List<FileToImport> collectInputs(final Path workspaceRoot,
            final OftIgnore ignore) {
        final List<FileToImport> files = new ArrayList<>();
        try {
            Files.walkFileTree(workspaceRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                    if (!dir.equals(workspaceRoot) && ignore.isExcluded(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                    if (OftWorkspaceIndex.isIndexedFile(ignore, file)) {
                        files.add(FileToImport.of(file, attrs));
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
            return List.of(FileToImport.unknown(workspaceRoot));
        }
        return files;
    }
}
