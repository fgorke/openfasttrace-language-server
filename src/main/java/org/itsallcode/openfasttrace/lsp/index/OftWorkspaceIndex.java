package org.itsallcode.openfasttrace.lsp.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;

// [impl->req~index-on-startup~1]
public final class OftWorkspaceIndex {

    private final Map<SpecificationItemId, SpecificationItem> specItems;
    private final Map<SpecificationItemId, List<SpecificationItem>> coverageBySpecId;

    public OftWorkspaceIndex(final List<SpecificationItem> items) {
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
    }

    public static OftWorkspaceIndex empty() {
        return new OftWorkspaceIndex(List.of());
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

    public Optional<SpecificationItem> findOutdatedTarget(final SpecificationItemId tagId) {
        return findSpecItemByTypeAndName(tagId.getArtifactType(), tagId.getName())
                .filter(specItem -> specItem.getId().getRevision() != tagId.getRevision());
    }

    public int specItemCount() {
        return specItems.size();
    }

    public List<SpecificationItem> allSpecItems() {
        return List.copyOf(specItems.values());
    }
}
