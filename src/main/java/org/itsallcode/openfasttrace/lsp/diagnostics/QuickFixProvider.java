package org.itsallcode.openfasttrace.lsp.diagnostics;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;

import com.google.gson.JsonPrimitive;

// [impl->req~quickfix-updates-version~1]
public class QuickFixProvider {

    public List<CodeAction> quickFixesForDiagnostic(final Diagnostic diagnostic,
            final String fileUri) {
        final String newId = correctedIdFrom(diagnostic);
        if (newId == null) {
            return Collections.emptyList();
        }

        final var edit = new TextEdit(diagnostic.getRange(), newId);
        final Map<String, List<TextEdit>> changes = new HashMap<>();
        changes.put(fileUri, List.of(edit));

        final var action = new CodeAction("Update to " + newId);
        action.setKind(CodeActionKind.QuickFix);
        action.setDiagnostics(List.of(diagnostic));
        action.setEdit(new WorkspaceEdit(changes));
        return List.of(action);
    }

    private static String correctedIdFrom(final Diagnostic diagnostic) {
        final Object data = diagnostic.getData();
        if (data instanceof String string) {
            return string;
        }
        if (data instanceof JsonPrimitive primitive) {
            return primitive.getAsString();
        }
        return null;
    }
}
