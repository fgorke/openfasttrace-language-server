package org.itsallcode.openfasttrace.lsp.decisions;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.itsallcode.openfasttrace.lsp.OftSyntax;
import org.itsallcode.openfasttrace.lsp.index.LocationConverter;

// [impl->req~generate-specification-item-id-for-adr~1]
// [impl->adr~trace-architecture-decision-records-as-specification-items~1]
public final class AdrItemIdAction {

    public static final String ARTIFACT_TYPE = "adr";

    private static final List<String> DECISION_DIRECTORIES = List.of("decisions", "adr");

    private static final Pattern TITLE_LINE = Pattern.compile("^#\\s+\\S.*$");

    private static final Pattern LEADING_NUMBER = Pattern.compile("^\\d+[-_.]?");

    private AdrItemIdAction() {
    }

    public static Optional<TextEdit> idEditFor(final String uri, final List<String> lines,
            final int lineIndex) {
        if (!isTitleOfUnnumberedRecord(uri, lines, lineIndex)) {
            return Optional.empty();
        }
        return idTextFor(uri).map(id -> new TextEdit(afterLine(lineIndex), "\n`" + id + "`"));
    }

    public static Optional<String> idTextFor(final String uri) {
        return itemNameOf(uri).map(name -> ARTIFACT_TYPE + "~" + name + "~1");
    }

    private static boolean isTitleOfUnnumberedRecord(final String uri, final List<String> lines,
            final int lineIndex) {
        return isDecisionRecord(uri) && isTitle(lines, lineIndex) && !hasItemId(lines);
    }

    static boolean isDecisionRecord(final String uri) {
        return LocationConverter.toPath(uri)
                .filter(path -> OftSyntax.isMarkdown(String.valueOf(path.getFileName())))
                .map(Path::getParent)
                .map(directory -> String.valueOf(directory.getFileName()).toLowerCase(Locale.ROOT))
                .filter(DECISION_DIRECTORIES::contains)
                .isPresent();
    }

    static Optional<String> itemNameOf(final String uri) {
        return LocationConverter.toPath(uri)
                .map(path -> LEADING_NUMBER
                        .matcher(withoutExtension(String.valueOf(path.getFileName())))
                        .replaceFirst(""))
                .filter(OftSyntax::isValidItemName);
    }

    private static String withoutExtension(final String fileName) {
        final int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    private static boolean isTitle(final List<String> lines, final int lineIndex) {
        return lineIndex >= 0 && lineIndex < lines.size()
                && TITLE_LINE.matcher(lines.get(lineIndex)).matches();
    }

    private static boolean hasItemId(final List<String> lines) {
        return lines.stream().anyMatch(
                line -> OftSyntax.SPECIFICATION_ITEM_DEFINITION_LINE.matcher(line).matches());
    }

    private static Range afterLine(final int lineIndex) {
        final Position endOfTitle = new Position(lineIndex, Integer.MAX_VALUE);
        return new Range(endOfTitle, endOfTitle);
    }
}
