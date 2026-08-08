package org.itsallcode.openfasttrace.lsp.codelens;

import java.util.List;
import java.util.Optional;

import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.itsallcode.openfasttrace.api.core.LinkedSpecificationItem;
import org.itsallcode.openfasttrace.lsp.symbols.OftSymbolProvider;

// [impl->req~coverage-code-lens~1]
public final class OftCodeLensProvider {

    private OftCodeLensProvider() {
    }

    public static List<CodeLens> codeLenses(final List<LinkedSpecificationItem> itemsInFile) {
        return itemsInFile.stream()
                .map(OftCodeLensProvider::codeLensFor)
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<CodeLens> codeLensFor(final LinkedSpecificationItem item) {
        if (item.getLocation() == null || OftSymbolProvider.isCoverageTag(item.getItem())) {
            return Optional.empty();
        }
        return summaryOf(item).map(summary -> toCodeLens(item, summary));
    }

    private static Optional<String> summaryOf(final LinkedSpecificationItem item) {
        final List<String> uncovered = item.getUncoveredArtifactTypes();
        final String covered = coveredTypesOf(item);
        if (!uncovered.isEmpty()) {
            final String missing = "missing " + String.join(", ", uncovered);
            return Optional.of(covered.isEmpty() ? missing : missing + " · " + covered);
        }
        return covered.isEmpty() ? Optional.empty() : Optional.of(covered);
    }

    private static String coveredTypesOf(final LinkedSpecificationItem item) {
        final List<String> types = item.getCoveredArtifactTypes().stream().sorted().toList();
        return types.isEmpty() ? "" : "covered by " + String.join(", ", types);
    }

    private static CodeLens toCodeLens(final LinkedSpecificationItem item, final String summary) {
        final int line = Math.max(0, item.getLocation().getLine() - 1);
        final Range range = new Range(new Position(line, 0), new Position(line, 0));
        return new CodeLens(range, new Command(summary, ""), null);
    }
}
