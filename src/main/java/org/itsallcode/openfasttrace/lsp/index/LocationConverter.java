package org.itsallcode.openfasttrace.lsp.index;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.itsallcode.openfasttrace.api.core.Location;
import org.itsallcode.openfasttrace.api.core.SourceRange;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.lsp.OftSyntax;

public final class LocationConverter {

    private LocationConverter() {
    }

    public static org.eclipse.lsp4j.Location toLspLocation(final Location oftLocation) {
        return toLspLocation(oftLocation, null);
    }

    // [impl->req~precise-ranges-from-oft~1]
    public static Optional<Range> toLspRange(final SourceRange range) {
        if (range == null || range.getStart() == null || range.getEnd() == null) {
            return Optional.empty();
        }
        return Optional.of(new Range(
                new Position(range.getStart().getLine(), range.getStart().getColumn()),
                new Position(range.getEnd().getLine(), range.getEnd().getColumn())));
    }

    // [impl->req~precise-ranges-from-oft~1]
    public static Optional<Range> rangeOfDeclaredId(final SpecificationItem item) {
        return Optional.ofNullable(item.getLocatedId())
                .flatMap(located -> toLspRange(located.getRange()));
    }

    public static org.eclipse.lsp4j.Location toLspLocation(final Location oftLocation,
            final String lineText) {
        final String uri = pathToUri(oftLocation.getPath());
        final int line = Math.max(0, oftLocation.getLine() - 1);
        final int[] span = highlightSpan(lineText);
        final int startCol = span == null ? 0 : span[0];
        final int endCol = span == null ? Integer.MAX_VALUE : span[1];
        return new org.eclipse.lsp4j.Location(uri,
                new Range(new Position(line, startCol), new Position(line, endCol)));
    }

    private static int[] highlightSpan(final String lineText) {
        if (lineText == null || lineText.isEmpty()) {
            return null;
        }
        final Matcher tag = OftSyntax.COVERAGE_TAG_LOOSE.matcher(lineText);
        if (tag.find()) {
            return new int[] { tag.start(), tag.end() };
        }
        final Matcher id = OftSyntax.SPECIFICATION_ITEM_ID.matcher(lineText);
        if (id.find()) {
            return new int[] { id.start(), id.end() };
        }
        return null;
    }

    public static String pathToUri(final String path) {
        if (path.startsWith("file:")) {
            return path;
        }
        if (path.startsWith("/")) {
            return "file://" + path;
        }
        return Path.of(path).toUri().toString();
    }

    public static String toFileKey(final String uriOrPath) {
        final Path path;
        try {
            path = uriOrPath.startsWith("file:") ? Path.of(URI.create(uriOrPath)) : Path.of(uriOrPath);
        } catch (final RuntimeException exception) {
            return uriOrPath;
        }
        try {
            return path.toRealPath().toString();
        } catch (final IOException exception) {
            return path.normalize().toAbsolutePath().toString();
        }
    }
}
