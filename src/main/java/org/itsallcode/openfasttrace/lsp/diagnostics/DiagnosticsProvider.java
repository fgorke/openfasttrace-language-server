package org.itsallcode.openfasttrace.lsp.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.OftSyntax;

// [impl->req~diagnostic-outdated-version~1]
public class DiagnosticsProvider {

    public List<Diagnostic> diagnoseLines(final List<String> lines,
            final OftWorkspaceIndex index) {
        final List<Diagnostic> diagnostics = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            diagnostics.addAll(diagnoseLine(lines.get(lineIndex), lineIndex, index));
        }
        return diagnostics;
    }

    private List<Diagnostic> diagnoseLine(final String lineText, final int lineIndex,
            final OftWorkspaceIndex index) {
        final List<Diagnostic> diagnostics = new ArrayList<>();
        final Matcher matcher = OftSyntax.SPECIFICATION_ITEM_ID.matcher(lineText);
        while (matcher.find()) {
            final SpecificationItemId tagId = SpecificationItemId.parseId(matcher.group());
            findOutdatedDiagnostic(tagId, lineIndex, matcher.start(), matcher.end(), index)
                    .ifPresent(diagnostics::add);
        }
        return diagnostics;
    }

    private Optional<Diagnostic> findOutdatedDiagnostic(final SpecificationItemId tagId,
            final int line, final int startCol, final int endCol,
            final OftWorkspaceIndex index) {
        return index.findOutdatedTarget(tagId)
                .map(specItem -> buildDiagnostic(tagId, specItem, line, startCol, endCol));
    }

    private Diagnostic buildDiagnostic(final SpecificationItemId tagId,
            final SpecificationItem specItem, final int line,
            final int startCol, final int endCol) {
        final String currentId = specItem.getId().toString();
        final var range = new Range(new Position(line, startCol), new Position(line, endCol));
        final String message = String.format(
                "Outdated reference: '%s', current revision is %d",
                tagId, specItem.getId().getRevision());
        final var diagnostic = new Diagnostic(range, message, DiagnosticSeverity.Warning,
                "openfasttrace-lsp");
        diagnostic.setData(currentId);
        return diagnostic;
    }
}
