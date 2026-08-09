package org.itsallcode.openfasttrace.lsp.index;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.itsallcode.openfasttrace.api.importer.ImportSettings;
import org.itsallcode.openfasttrace.api.importer.ImporterContext;
import org.itsallcode.openfasttrace.api.importer.ImporterFactory;
import org.itsallcode.openfasttrace.api.importer.input.InputFile;
import org.itsallcode.openfasttrace.api.importer.input.RealFileInput;

// [impl->req~supported-files-from-oft~1]
public final class OftSupportedFiles {

    private static final List<ImporterFactory> FACTORIES = loadFactories();

    private OftSupportedFiles() {
    }

    private static List<ImporterFactory> loadFactories() {
        final List<ImporterFactory> factories = new ArrayList<>();
        ServiceLoader.load(ImporterFactory.class).forEach(factories::add);
        final ImporterContext context = new ImporterContext(ImportSettings.createDefault());
        factories.forEach(factory -> factory.init(context));
        return List.copyOf(factories);
    }

    public static boolean isSupported(final Path file) {
        final InputFile input = RealFileInput.forPath(file);
        return FACTORIES.stream().anyMatch(factory -> factory.supportsFile(input));
    }
}
