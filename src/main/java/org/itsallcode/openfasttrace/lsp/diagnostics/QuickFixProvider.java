package org.itsallcode.openfasttrace.lsp.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.OftSyntax;

import com.google.gson.JsonPrimitive;

// [impl->req~quickfix-updates-version~1]
public class QuickFixProvider {

    private static final int ARTIFACT_TYPE_GROUP = 1;
    private static final int NAME_GROUP = 2;
    private static final int REVISION_GROUP = 3;

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

    public static Optional<SpecificationItemId> outdatedTargetOf(final Diagnostic diagnostic) {
        final String correctedId = correctedIdFrom(diagnostic);
        if (correctedId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SpecificationItemId.parseId(correctedId));
        } catch (final RuntimeException exception) {
            return Optional.empty();
        }
    }

    // [impl->req~quickfix-updates-all-versions~1]
    public static List<TextEdit> revisionUpdatesInLine(final String line, final int lineIndex,
            final SpecificationItemId currentId) {
        final List<TextEdit> edits = new ArrayList<>();
        final Matcher matcher = OftSyntax.SPECIFICATION_ITEM_ID.matcher(line);
        while (matcher.find()) {
            if (matcher.group(ARTIFACT_TYPE_GROUP).equals(currentId.getArtifactType())
                    && matcher.group(NAME_GROUP).equals(currentId.getName())
                    && !matcher.group(REVISION_GROUP).equals(revisionOf(currentId))) {
                edits.add(new TextEdit(
                        new Range(new Position(lineIndex, matcher.start(REVISION_GROUP)),
                                new Position(lineIndex, matcher.end(REVISION_GROUP))),
                        revisionOf(currentId)));
            }
        }
        return edits;
    }

    private static String revisionOf(final SpecificationItemId id) {
        return String.valueOf(id.getRevision());
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
