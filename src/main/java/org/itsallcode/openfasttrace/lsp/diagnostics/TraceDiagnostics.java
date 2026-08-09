package org.itsallcode.openfasttrace.lsp.diagnostics;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.itsallcode.openfasttrace.api.core.DeepCoverageStatus;
import org.itsallcode.openfasttrace.api.core.LinkStatus;
import org.itsallcode.openfasttrace.api.core.LinkedSpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.index.LocationConverter;
import org.itsallcode.openfasttrace.lsp.OftSyntax;

// [impl->req~diagnostic-trace-defects~1]
public final class TraceDiagnostics {

    private static final String SOURCE = "openfasttrace-lsp";

    private static final Set<LinkStatus> BAD_OUTGOING_LINKS = EnumSet.of(
            LinkStatus.ORPHANED, LinkStatus.OUTDATED, LinkStatus.PREDATED,
            LinkStatus.AMBIGUOUS, LinkStatus.UNWANTED);

    private TraceDiagnostics() {
    }

    public static List<Diagnostic> diagnose(final List<LinkedSpecificationItem> defects,
            final List<String> lines) {
        final List<Diagnostic> diagnostics = new ArrayList<>();
        for (final LinkedSpecificationItem defect : defects) {
            diagnostics.addAll(diagnoseItem(defect, lines));
        }
        return diagnostics;
    }

    private static List<Diagnostic> diagnoseItem(final LinkedSpecificationItem defect,
            final List<String> lines) {
        final int lineIndex = lineIndexOf(defect);
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return List.of();
        }
        final String line = lines.get(lineIndex);
        final List<Diagnostic> diagnostics = new ArrayList<>();
        diagnostics.addAll(badLinkDiagnostics(defect, line, lineIndex));
        coverageDiagnostic(defect, line, lineIndex).ifPresent(diagnostics::add);
        duplicateDiagnostic(defect, line, lineIndex).ifPresent(diagnostics::add);
        return diagnostics;
    }

    private static int lineIndexOf(final LinkedSpecificationItem defect) {
        return defect.getLocation() == null ? -1 : defect.getLocation().getLine() - 1;
    }

    private static List<Diagnostic> badLinkDiagnostics(final LinkedSpecificationItem defect,
            final String line, final int lineIndex) {
        final List<Diagnostic> diagnostics = new ArrayList<>();
        defect.getLinks().forEach((status, targets) -> {
            if (!BAD_OUTGOING_LINKS.contains(status)) {
                return;
            }
            for (final LinkedSpecificationItem target : targets) {
                if (isSupersededOrphan(defect, status, target.getId())) {
                    continue;
                }
                diagnostics.add(badLinkDiagnostic(defect, status, target.getId(), line, lineIndex));
            }
        });
        return diagnostics;
    }

    private static boolean isSupersededOrphan(final LinkedSpecificationItem defect,
            final LinkStatus status, final SpecificationItemId targetId) {
        if (status != LinkStatus.ORPHANED) {
            return false;
        }
        return hasLinkToSameItem(defect, LinkStatus.OUTDATED, targetId)
                || hasLinkToSameItem(defect, LinkStatus.PREDATED, targetId);
    }

    private static boolean hasLinkToSameItem(final LinkedSpecificationItem defect,
            final LinkStatus status, final SpecificationItemId targetId) {
        return defect.getLinksByStatus(status).stream()
                .anyMatch(other -> sameItem(other.getId(), targetId));
    }

    private static boolean sameItem(final SpecificationItemId left, final SpecificationItemId right) {
        return left.getArtifactType().equals(right.getArtifactType())
                && left.getName().equals(right.getName());
    }

    private static Diagnostic badLinkDiagnostic(final LinkedSpecificationItem defect,
            final LinkStatus status, final SpecificationItemId targetId, final String line,
            final int lineIndex) {
        final Range range = rangeOfCoveredId(defect, targetId, line, lineIndex);
        final Diagnostic diagnostic = switch (status) {
            case ORPHANED -> new Diagnostic(range,
                    "Covers '" + targetId + "', which does not exist.",
                    DiagnosticSeverity.Warning, SOURCE);
            case OUTDATED -> outdatedDiagnostic(range, targetId);
            case PREDATED -> new Diagnostic(range,
                    "Covers a revision of '" + targetId.getArtifactType() + "~"
                            + targetId.getName() + "' that is newer than the item itself (revision "
                            + targetId.getRevision() + ").",
                    DiagnosticSeverity.Warning, SOURCE);
            case AMBIGUOUS -> new Diagnostic(range,
                    "Covers '" + targetId + "', which is defined more than once.",
                    DiagnosticSeverity.Warning, SOURCE);
            default -> new Diagnostic(range,
                    "Covers '" + targetId + "', which does not want this coverage.",
                    DiagnosticSeverity.Warning, SOURCE);
        };
        if (defect.isTransitiveDefect()) {
            diagnostic.setSeverity(DiagnosticSeverity.Information);
        }
        return diagnostic;
    }

    // [impl->req~diagnostic-outdated-version~1]
    private static Diagnostic outdatedDiagnostic(final Range range,
            final SpecificationItemId currentId) {
        final Diagnostic diagnostic = new Diagnostic(range,
                "Outdated reference: current revision of '" + currentId.getArtifactType() + "~"
                        + currentId.getName() + "' is " + currentId.getRevision() + ".",
                DiagnosticSeverity.Warning, SOURCE);
        diagnostic.setData(currentId.toString());
        return diagnostic;
    }

    private static Optional<Diagnostic> coverageDiagnostic(final LinkedSpecificationItem defect,
            final String line, final int lineIndex) {
        final List<String> uncovered = defect.getUncoveredArtifactTypes();
        if (!uncovered.isEmpty()) {
            return Optional.of(new Diagnostic(rangeOfOwnId(defect, line, lineIndex),
                    "Not covered by: " + String.join(", ", uncovered) + ".",
                    DiagnosticSeverity.Warning, SOURCE));
        }
        if (defect.getDeepCoverageStatus() == DeepCoverageStatus.UNCOVERED
                && defect.isTransitiveDefect()) {
            return Optional.of(new Diagnostic(rangeOfOwnId(defect, line, lineIndex),
                    "Not covered all the way down: " + incompleteCoveringItems(defect) + ".",
                    DiagnosticSeverity.Information, SOURCE));
        }
        if (defect.getDeepCoverageStatus() == DeepCoverageStatus.CYCLE) {
            return Optional.of(new Diagnostic(rangeOfOwnId(defect, line, lineIndex),
                    "Coverage of this item forms a cycle.",
                    DiagnosticSeverity.Warning, SOURCE));
        }
        return Optional.empty();
    }

    private static String incompleteCoveringItems(final LinkedSpecificationItem defect) {
        final String items = defect.getLinksByStatus(LinkStatus.COVERED_SHALLOW).stream()
                .filter(LinkedSpecificationItem::isDefect)
                .map(item -> item.getId().toString())
                .collect(Collectors.joining(", "));
        return items.isEmpty() ? "a covering item is incomplete" : items + " is incomplete";
    }

    private static Optional<Diagnostic> duplicateDiagnostic(final LinkedSpecificationItem defect,
            final String line, final int lineIndex) {
        if (!defect.hasDuplicates()) {
            return Optional.empty();
        }
        return Optional.of(new Diagnostic(rangeOfOwnId(defect, line, lineIndex),
                "'" + defect.getId() + "' is defined more than once.",
                DiagnosticSeverity.Warning, SOURCE));
    }

    // [impl->req~precise-ranges-from-oft~1]
    private static Range rangeOfOwnId(final LinkedSpecificationItem defect, final String line,
            final int lineIndex) {
        return LocationConverter.rangeOfDeclaredId(defect.getItem())
                .orElseGet(() -> scanForId(line, lineIndex, defect.getId()));
    }

    // [impl->req~precise-ranges-from-oft~1]
    private static Range rangeOfCoveredId(final LinkedSpecificationItem defect,
            final SpecificationItemId targetId, final String line, final int lineIndex) {
        return defect.getItem().getLocatedCoveredIds().stream()
                .filter(located -> sameItem(located.getId(), targetId))
                .findFirst()
                .flatMap(located -> LocationConverter.toLspRange(located.getRange()))
                .orElseGet(() -> scanForId(line, lineIndex, targetId));
    }

    //fallback for when OFT does not provide a range for the id, which can happen for some file types
    private static Range scanForId(final String line, final int lineIndex,
            final SpecificationItemId id) {
        final Matcher matcher = OftSyntax.SPECIFICATION_ITEM_ID.matcher(line);
        while (matcher.find()) {
            if (id.getArtifactType().equals(matcher.group(1))
                    && id.getName().equals(matcher.group(2))) {
                return new Range(new Position(lineIndex, matcher.start()),
                        new Position(lineIndex, matcher.end()));
            }
        }
        return new Range(new Position(lineIndex, 0), new Position(lineIndex, line.length()));
    }
}
