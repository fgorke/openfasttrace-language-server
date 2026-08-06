package org.itsallcode.openfasttrace.lsp.rename;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.OftSyntax;

// [impl->req~rename-name-part-only~1]
public final class OftRenameProvider {

    private static final int NAME_GROUP = 2;
    private static final int ARTIFACT_TYPE_GROUP = 1;

    private static final Pattern ITEM_NAME =
            Pattern.compile("^" + SpecificationItemId.ITEM_NAME_PATTERN + "$");

    private OftRenameProvider() {
    }

    // [impl->req~prepare-rename~1]
    public static Optional<Range> nameRangeAt(final String line, final int lineIndex, final int col) {
        return findIdAt(line, col)
                .map(matcher -> nameRange(matcher, lineIndex));
    }

    private static Optional<Matcher> findIdAt(final String line, final int col) {
        final Matcher matcher = OftSyntax.SPECIFICATION_ITEM_ID.matcher(line);
        while (matcher.find()) {
            if (matcher.start() <= col && col < matcher.end()) {
                return Optional.of(matcher);
            }
        }
        return Optional.empty();
    }

    private static Range nameRange(final Matcher matcher, final int lineIndex) {
        return new Range(new Position(lineIndex, matcher.start(NAME_GROUP)),
                new Position(lineIndex, matcher.end(NAME_GROUP)));
    }

    public static String extractItemName(final String newName) {
        final String trimmed = newName == null ? "" : newName.strip();
        final Matcher completeId = OftSyntax.SPECIFICATION_ITEM_ID.matcher(trimmed);
        if (completeId.matches()) {
            return completeId.group(NAME_GROUP);
        }
        return trimmed;
    }

    public static boolean isValidItemName(final String name) {
        return ITEM_NAME.matcher(name).matches();
    }

    // [impl->req~rename-specification-item~1]
    public static List<TextEdit> renameEditsInLine(final String line, final int lineIndex,
            final String artifactType, final String oldName, final String newName) {
        final List<TextEdit> edits = new ArrayList<>();
        final Matcher matcher = OftSyntax.SPECIFICATION_ITEM_ID.matcher(line);
        while (matcher.find()) {
            if (artifactType.equals(matcher.group(ARTIFACT_TYPE_GROUP))
                    && oldName.equals(matcher.group(NAME_GROUP))) {
                edits.add(new TextEdit(nameRange(matcher, lineIndex), newName));
            }
        }
        return edits;
    }
}
