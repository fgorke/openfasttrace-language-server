package org.itsallcode.openfasttrace.lsp.diagnostics;

import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;

// [impl->req~diagnostic-trace-defects~1]
public class DiagnosticsProvider {

    public List<Diagnostic> diagnoseFile(final String uri, final List<String> lines,
            final OftWorkspaceIndex index) {
        return TraceDiagnostics.diagnose(index.defectsInFile(uri), lines);
    }
}
