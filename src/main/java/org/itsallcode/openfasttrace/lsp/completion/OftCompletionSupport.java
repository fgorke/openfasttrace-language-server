package org.itsallcode.openfasttrace.lsp.completion;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;

// [impl->req~complete-specification-item-id-in-coverage-tag-target~1, req~complete-specification-item-id-in-covers-section~1]
public final class OftCompletionSupport {

    private OftCompletionSupport() {
    }

    public enum MatchKind {
        FULL_ID_PREFIX,
        NAME_PREFIX,
        NAME_SUBSTRING,
        ARTIFACT_TYPE_PREFIX,
        NONE
    }

    public static List<SpecificationItem> findMatching(final OftWorkspaceIndex index, final String prefix,
            final String coveringArtifactType) {
        return index.allSpecItems().stream()
                .filter(OftCompletionSupport::hasTitle)
                .filter(item -> coveringArtifactType == null
                        || item.needsCoverageByArtifactType(coveringArtifactType))
                .filter(item -> matchKind(item, prefix) != MatchKind.NONE)
                .sorted(Comparator.comparing((SpecificationItem item) -> matchKind(item, prefix))
                        .thenComparing(item -> item.getId().toString()))
                .collect(Collectors.toList());
    }

    private static boolean hasTitle(final SpecificationItem item) {
        return item.getTitle() != null && !item.getTitle().isBlank();
    }

    public static MatchKind matchKind(final SpecificationItem item, final String prefix) {
        final String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        final String id = item.getId().toString().toLowerCase(Locale.ROOT);
        if (id.startsWith(normalizedPrefix)) {
            return MatchKind.FULL_ID_PREFIX;
        }
        final String name = item.getId().getName().toLowerCase(Locale.ROOT);
        if (name.startsWith(normalizedPrefix)) {
            return MatchKind.NAME_PREFIX;
        }
        if (name.contains(normalizedPrefix)) {
            return MatchKind.NAME_SUBSTRING;
        }
        final String artifactType = item.getId().getArtifactType().toLowerCase(Locale.ROOT);
        if (artifactType.startsWith(normalizedPrefix)) {
            return MatchKind.ARTIFACT_TYPE_PREFIX;
        }
        return MatchKind.NONE;
    }

    public static String sortTextFor(final SpecificationItem item, final String prefix) {
        return matchKind(item, prefix).ordinal() + "_" + item.getId();
    }
}
