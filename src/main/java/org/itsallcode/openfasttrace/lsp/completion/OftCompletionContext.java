package org.itsallcode.openfasttrace.lsp.completion;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import org.itsallcode.openfasttrace.lsp.OftSyntax;

public record OftCompletionContext(String prefix, boolean appendClosingBracket, String coveringArtifactType) {

    public static Optional<OftCompletionContext> findAt(final List<String> lines, final int lineIndex,
            final int col) {
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return Optional.empty();
        }
        final String line = lines.get(lineIndex);
        final int boundedCol = Math.min(Math.max(col, 0), line.length());

        return findInOpenCoverageTag(line, boundedCol)
                .or(() -> findInCoversSection(lines, lineIndex, boundedCol));
    }

    private static Optional<OftCompletionContext> findInOpenCoverageTag(final String line, final int col) {
        final int bracketStart = line.lastIndexOf('[', Math.max(0, col - 1));
        if (bracketStart < 0 || !hasCommentMarkerBefore(line, bracketStart)) {
            return Optional.empty();
        }
        final int closingBracket = line.indexOf(']', bracketStart + 1);
        if (closingBracket >= 0 && closingBracket < col) {
            return Optional.empty();
        }
        final int arrowStart = line.lastIndexOf("->", Math.max(bracketStart, col - 1));
        if (arrowStart < bracketStart) {
            return Optional.empty();
        }
        final int targetStart = arrowStart + 2;
        if (targetStart > col) {
            return Optional.empty();
        }
        final String between = line.substring(targetStart, col);
        if (!between.matches("\\s*" + OftSyntax.ID_CHARACTER_CLASS + "*")) {
            return Optional.empty();
        }
        return Optional.of(new OftCompletionContext(between.strip(), !hasClosingBracketAhead(line, col),
                coveringArtifactTypeOf(line, bracketStart, arrowStart)));
    }

    private static String coveringArtifactTypeOf(final String line, final int bracketStart,
            final int arrowStart) {
        final String source = line.substring(bracketStart + 1, arrowStart).strip();
        final int nameSeparator = source.indexOf('~');
        final String artifactType = nameSeparator < 0 ? source : source.substring(0, nameSeparator);
        return artifactType.matches("\\p{Alpha}+") ? artifactType : null;
    }

    private static boolean hasCommentMarkerBefore(final String line, final int bracketStart) {
        final String before = line.substring(0, bracketStart).strip();
        return OftSyntax.COMMENT_MARKERS.stream().anyMatch(before::endsWith);
    }

    private static boolean hasClosingBracketAhead(final String line, final int col) {
        int i = col;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return i < line.length() && line.charAt(i) == ']';
    }

    private static Optional<OftCompletionContext> findInCoversSection(final List<String> lines,
            final int lineIndex, final int col) {
        boolean insideCoversSection = false;
        for (int i = 0; i <= lineIndex; i++) {
            insideCoversSection = updateSectionState(lines.get(i), insideCoversSection);
        }
        if (!insideCoversSection) {
            return Optional.empty();
        }
        return Optional.of(new OftCompletionContext(prefixBefore(lines.get(lineIndex), col), false, null));
    }

    private static boolean updateSectionState(final String line, final boolean insideCoversSection) {
        if (line.isBlank()) {
            return insideCoversSection;
        }
        if (OftSyntax.SPECIFICATION_ITEM_DEFINITION_LINE.matcher(line).matches()) {
            return false;
        }
        final Matcher keyword = OftSyntax.SECTION_KEYWORD_LINE.matcher(line);
        if (keyword.find()) {
            return "Covers".equals(keyword.group(1));
        }
        return insideCoversSection;
    }

    private static String prefixBefore(final String line, final int col) {
        int start = col;
        while (start > 0 && line.substring(start - 1, start).matches(OftSyntax.ID_CHARACTER_CLASS)) {
            start--;
        }
        return line.substring(start, col);
    }

    public static boolean isInsideCommentWithoutOpenTag(final String line, final int col) {
        final int boundedCol = Math.min(Math.max(col, 0), line.length());
        final String beforeCursor = line.substring(0, boundedCol);
        if (OftSyntax.COMMENT_STARTERS.stream().noneMatch(beforeCursor::contains)) {
            return false;
        }
        final int bracketStart = line.lastIndexOf('[', Math.max(0, boundedCol - 1));
        if (bracketStart < 0) {
            return true;
        }
        final int closingBracket = line.indexOf(']', bracketStart + 1);
        final boolean stillOpen = closingBracket < 0 || closingBracket >= boundedCol;
        return !stillOpen;
    }

}
