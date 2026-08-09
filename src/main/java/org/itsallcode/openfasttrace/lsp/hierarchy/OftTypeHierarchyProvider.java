package org.itsallcode.openfasttrace.lsp.hierarchy;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;

import com.google.gson.JsonPrimitive;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.itsallcode.openfasttrace.api.core.Location;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.index.LocationConverter;
import org.itsallcode.openfasttrace.lsp.index.OftIdAtPosition;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.symbols.OftSymbolProvider;

// [impl->req~coverage-hierarchy~2]
public final class OftTypeHierarchyProvider {

    private OftTypeHierarchyProvider() {
    }

    public static List<TypeHierarchyItem> prepareAt(final String lineText, final int col,
            final OftWorkspaceIndex index) {
        return OftIdAtPosition.findAt(lineText, col)
                .flatMap(id -> resolve(id, index))
                .map(item -> rootsOf(item, index))
                .orElseGet(List::of);
    }

    private static List<TypeHierarchyItem> rootsOf(final SpecificationItem item,
            final OftWorkspaceIndex index) {
        final Map<SpecificationItemId, SpecificationItem> roots = new LinkedHashMap<>();
        collectRoots(item, index, new LinkedHashSet<>(), roots);
        final Collection<SpecificationItem> found = roots.isEmpty() ? List.of(item) : roots.values();
        return found.stream()
                .sorted(forDisplay())
                .map(OftTypeHierarchyProvider::toHierarchyItem)
                .toList();
    }

    private static void collectRoots(final SpecificationItem item, final OftWorkspaceIndex index,
            final Set<SpecificationItemId> visited,
            final Map<SpecificationItemId, SpecificationItem> roots) {
        if (!visited.add(item.getId())) {
            return;
        }
        final List<SpecificationItem> covered = item.getCoveredIds().stream()
                .map(id -> resolve(id, index))
                .flatMap(Optional::stream)
                .toList();
        if (covered.isEmpty()) {
            roots.put(item.getId(), item);
            return;
        }
        covered.forEach(parent -> collectRoots(parent, index, visited, roots));
    }

    public static List<TypeHierarchyItem> supertypesOf(final TypeHierarchyItem item,
            final OftWorkspaceIndex index) {
        return specItemOf(item, index)
                .map(SpecificationItem::getCoveredIds)
                .orElseGet(List::of)
                .stream()
                .map(id -> resolve(id, index))
                .flatMap(Optional::stream)
                .sorted(forDisplay())
                .map(OftTypeHierarchyProvider::toHierarchyItem)
                .toList();
    }

    public static List<TypeHierarchyItem> subtypesOf(final TypeHierarchyItem item,
            final OftWorkspaceIndex index) {
        return specItemOf(item, index)
                .map(specItem -> index.findCoverageTags(specItem.getId()))
                .orElseGet(List::of)
                .stream()
                .sorted(forDisplay())
                .map(OftTypeHierarchyProvider::toHierarchyItem)
                .toList();
    }

    private static Comparator<SpecificationItem> forDisplay() {
        return Comparator.comparing((SpecificationItem item) -> item.getId().getArtifactType())
                .thenComparing(OftTypeHierarchyProvider::fileOf)
                .thenComparingInt(OftTypeHierarchyProvider::lineOf)
                .thenComparing(item -> item.getId().getName());
    }

    private static String fileOf(final SpecificationItem item) {
        final Location location = item.getLocation();
        return location == null ? "" : String.valueOf(Path.of(location.getPath()).getFileName());
    }

    private static int lineOf(final SpecificationItem item) {
        final Location location = item.getLocation();
        return location == null ? 0 : location.getLine();
    }

    private static Optional<SpecificationItem> specItemOf(final TypeHierarchyItem item,
            final OftWorkspaceIndex index) {
        final Optional<SpecificationItem> byId = idOf(item).flatMap(id -> resolve(id, index));
        if (byId.isPresent()) {
            return byId;
        }
        return index.allSpecItems().stream()
                .filter(specItem -> isAt(specItem, item))
                .findFirst();
    }

    private static boolean isAt(final SpecificationItem specItem, final TypeHierarchyItem item) {
        final Location location = specItem.getLocation();
        return location != null
                && location.getLine() - 1 == item.getRange().getStart().getLine()
                && LocationConverter.toFileKey(location.getPath())
                        .equals(LocationConverter.toFileKey(item.getUri()));
    }

    private static Optional<SpecificationItemId> idOf(final TypeHierarchyItem item) {
        final Object data = item.getData();
        final String id = data instanceof JsonPrimitive primitive ? primitive.getAsString()
                : data instanceof String string ? string : null;
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SpecificationItemId.parseId(id));
        } catch (final RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static Optional<SpecificationItem> resolve(final SpecificationItemId id,
            final OftWorkspaceIndex index) {
        return index.findSpecItem(id)
                .or(() -> index.findSpecItemByTypeAndName(id.getArtifactType(), id.getName()));
    }

    private static TypeHierarchyItem toHierarchyItem(final SpecificationItem item) {
        final Location location = item.getLocation();
        final int line = location == null ? 0 : Math.max(0, location.getLine() - 1);
        final Range range = new Range(new Position(line, 0), new Position(line, 0));
        final TypeHierarchyItem hierarchyItem = new TypeHierarchyItem(
                nameOf(item), OftSymbolProvider.SYMBOL_KIND,
                location == null ? "" : LocationConverter.pathToUri(location.getPath()),
                range, range);
        hierarchyItem.setDetail(detailOf(item));
        hierarchyItem.setData(item.getId().toString());
        return hierarchyItem;
    }

    static String nameOf(final SpecificationItem item) {
        final SpecificationItemId id = item.getId();
        final String prefix = "[" + id.getArtifactType() + "] ";
        if (OftSymbolProvider.isCoverageTag(item)) {
            return prefix + fileNameOf(item);
        }
        return prefix + id.getName() + "~" + id.getRevision();
    }

    private static String fileNameOf(final SpecificationItem item) {
        final Location location = item.getLocation();
        if (location == null) {
            return item.getId().getName();
        }
        return Path.of(location.getPath()).getFileName() + ":" + location.getLine();
    }

    private static String detailOf(final SpecificationItem item) {
        return OftSymbolProvider.isCoverageTag(item) ? "" : fileOf(item);
    }
}
