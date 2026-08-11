package org.itsallcode.openfasttrace.lsp.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.itsallcode.openfasttrace.api.core.LinkedSpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;

// [impl->req~index-on-startup~1]
public final class OftWorkspaceIndex {

    private final Map<SpecificationItemId, SpecificationItem> specItems;
    private final Map<SpecificationItemId, List<SpecificationItem>> coverageBySpecId;
    private final Map<String, List<LinkedSpecificationItem>> linkedItemsByFile;
    private final OftIgnore ignore;

    public OftWorkspaceIndex(final List<SpecificationItem> items) {
        this(items, List.of(), OftIgnore.none());
    }

    private OftWorkspaceIndex(final List<SpecificationItem> items,
            final List<LinkedSpecificationItem> linkedItems, final OftIgnore ignore) {
        final Map<SpecificationItemId, SpecificationItem> specMap = new LinkedHashMap<>();
        final Map<SpecificationItemId, List<SpecificationItem>> coverMap = new LinkedHashMap<>();

        for (final SpecificationItem item : items) {
            specMap.put(item.getId(), item);
            for (final SpecificationItemId coveredId : item.getCoveredIds()) {
                coverMap.computeIfAbsent(coveredId, k -> new ArrayList<>()).add(item);
            }
        }

        this.specItems = Collections.unmodifiableMap(specMap);
        this.coverageBySpecId = Collections.unmodifiableMap(coverMap);
        this.linkedItemsByFile = groupByFile(linkedItems);
        this.ignore = ignore;
    }

    // [impl->req~diagnostic-trace-defects~1]
    public static OftWorkspaceIndex ofLinkedItems(final List<LinkedSpecificationItem> linkedItems,
            final OftIgnore ignore) {
        return new OftWorkspaceIndex(
                linkedItems.stream().map(LinkedSpecificationItem::getItem).toList(), linkedItems,
                ignore);
    }

    // [impl->req~index-ignore-file~1]
    public boolean isExcludedFile(final String uriOrPath) {
        return ignore.isExcluded(uriOrPath);
    }

    private static Map<String, List<LinkedSpecificationItem>> groupByFile(
            final List<LinkedSpecificationItem> linkedItems) {
        final Map<String, List<LinkedSpecificationItem>> byFile = new LinkedHashMap<>();
        for (final LinkedSpecificationItem item : linkedItems) {
            if (item.getLocation() == null) {
                continue;
            }
            final String key = LocationConverter.toFileKey(item.getLocation().getPath());
            byFile.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        return Collections.unmodifiableMap(byFile);
    }

    public static OftWorkspaceIndex empty() {
        return new OftWorkspaceIndex(List.of());
    }

    // [impl->req~trace-report-on-request~1]
    public List<LinkedSpecificationItem> allLinkedItems() {
        return linkedItemsByFile.values().stream().flatMap(List::stream).toList();
    }

    // [impl->req~coverage-code-lens~1]
    public List<LinkedSpecificationItem> linkedItemsInFile(final String uriOrPath) {
        return linkedItemsByFile.getOrDefault(LocationConverter.toFileKey(uriOrPath), List.of());
    }

    // [impl->req~diagnostic-trace-defects~1]
    public List<LinkedSpecificationItem> defectsInFile(final String uriOrPath) {
        return linkedItemsInFile(uriOrPath).stream()
                .filter(LinkedSpecificationItem::isDefect)
                .toList();
    }

    public Optional<SpecificationItem> findSpecItem(final SpecificationItemId id) {
        return Optional.ofNullable(specItems.get(id));
    }

    public List<SpecificationItem> findCoverageTags(final SpecificationItemId id) {
        return coverageBySpecId.getOrDefault(id, List.of());
    }

    public Optional<SpecificationItem> findSpecItemByTypeAndName(
            final String artifactType, final String name) {
        return specItems.values().stream()
                .filter(item -> artifactType.equals(item.getId().getArtifactType())
                        && name.equals(item.getId().getName()))
                .findFirst();
    }

    public int specItemCount() {
        return specItems.size();
    }

    public List<SpecificationItem> allSpecItems() {
        return List.copyOf(specItems.values());
    }

    // [impl->req~suggest-coverage-tag-start-in-comment~2]
    public List<String> neededArtifactTypes() {
        return specItems.values().stream()
                .flatMap(item -> item.getNeedsArtifactTypes().stream())
                .distinct()
                .sorted()
                .toList();
    }
}
