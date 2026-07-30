package org.itsallcode.openfasttrace.lsp.symbols;

import java.util.List;
import java.util.Locale;

import org.eclipse.lsp4j.SymbolKind;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;

// [impl->req~workspace-symbol-search~1]
// [impl->req~symbol-naming~1]
public final class OftSymbolProvider {

    public static final SymbolKind SYMBOL_KIND = SymbolKind.Class;

    private OftSymbolProvider() {
    }

    public static boolean isCoverageTag(final SpecificationItem item) {
        return !hasTitle(item) && !item.getCoveredIds().isEmpty();
    }

    private static boolean hasTitle(final SpecificationItem item) {
        return item.getTitle() != null && !item.getTitle().isBlank();
    }

    public static String nameOf(final SpecificationItem item) {
        return item.getId().toString();
    }

    public static String detailOf(final SpecificationItem item) {
        return hasTitle(item) ? item.getTitle() : "";
    }

    public static boolean matches(final SpecificationItem item, final String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        final String normalizedQuery = query.toLowerCase(Locale.ROOT);
        return nameOf(item).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || detailOf(item).toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    public static List<SpecificationItem> findMatching(final List<SpecificationItem> items,
            final String query) {
        return items.stream()
                .filter(item -> item.getLocation() != null)
                .filter(item -> !isCoverageTag(item))
                .filter(item -> matches(item, query))
                .sorted((left, right) -> nameOf(left).compareToIgnoreCase(nameOf(right)))
                .toList();
    }
}
