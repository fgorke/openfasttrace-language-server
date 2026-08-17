package org.itsallcode.openfasttrace.lsp.index;

import java.util.Optional;
import java.util.regex.Matcher;

import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.OftSyntax;

public final class OftIdAtPosition {

    private OftIdAtPosition() {
    }

    public record IdSpan(SpecificationItemId id, int startColumn, int endColumn) {
    }

    public static Optional<IdSpan> spanAt(final String line, final int col) {
        final Matcher matcher = OftSyntax.SPECIFICATION_ITEM_ID.matcher(line);
        while (matcher.find()) {
            if (matcher.start() <= col && col < matcher.end()) {
                return Optional.of(new IdSpan(SpecificationItemId.parseId(matcher.group()),
                        matcher.start(), matcher.end()));
            }
        }
        return Optional.empty();
    }

    public static Optional<SpecificationItemId> findAt(final String line, final int col) {
        return spanAt(line, col).map(IdSpan::id);
    }
}
