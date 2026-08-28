package org.itsallcode.openfasttrace.lsp;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.itsallcode.openfasttrace.api.core.SpecificationItemId;

// [impl->adr~centralize-oft-syntax-patterns-in-one-class~1]
public final class OftSyntax {

    public static final Pattern SPECIFICATION_ITEM_ID = SpecificationItemId.ID_PATTERN;

    public static final Pattern SPECIFICATION_ITEM_DEFINITION_LINE = Pattern.compile(
            "^`?(" + SPECIFICATION_ITEM_ID.pattern() + ")`?.*$");

    private static final Pattern ITEM_NAME =
            Pattern.compile("^" + SpecificationItemId.ITEM_NAME_PATTERN + "$");

    public static final Pattern SECTION_KEYWORD_LINE = Pattern.compile(
            "^\\s*(Needs|Covers|Depends|Status|Description|Rationale|Comment|Tags):");

    private static final String COVERED_ID_LIST = SPECIFICATION_ITEM_ID.pattern()
            + "(?:\\s*,\\s*" + SPECIFICATION_ITEM_ID.pattern() + ")*";

    private static final String OPTIONAL_NAME_AND_REVISION =
            "(?:" + SpecificationItemId.ARTIFACT_TYPE_SEPARATOR
                    + "(?:" + SpecificationItemId.ITEM_NAME_PATTERN + ")?"
                    + SpecificationItemId.REVISION_SEPARATOR
                    + SpecificationItemId.ITEM_REVISION_PATTERN + ")?";

    private static final String OPTIONAL_NEEDED_COVERAGE =
            "(?:>>\\s*\\p{Alpha}+(?:\\s*,\\s*\\p{Alpha}+)*\\s*)?";

    public static final Pattern COVERAGE_TAG = Pattern.compile(
            "\\[\\s*\\p{Alpha}+" + OPTIONAL_NAME_AND_REVISION + "\\s*->\\s*"
                    + COVERED_ID_LIST + "\\s*" + OPTIONAL_NEEDED_COVERAGE + "]");

    public static final Pattern COVERAGE_TAG_LOOSE = Pattern.compile("\\[[^\\]\\n]*->[^\\]\\n]*]");

    public static final String ID_CHARACTER_CLASS = "[\\p{L}\\p{N}~._-]";

    public static final List<String> COMMENT_MARKERS =
            List.of("//", "#", "--", ";", "/*", "<!--", "*");

    public static final List<String> COMMENT_STARTERS =
            List.of("//", "#", "--", ";", "/*", "<!--");

    public static final List<String> LINE_START_COMMENT_MARKERS = List.of("'", "..");

    private record CommentStyle(List<String> starters, List<String> lineStartMarkers) {
    }

    private static final CommentStyle CODE =
            new CommentStyle(COMMENT_STARTERS, LINE_START_COMMENT_MARKERS);

    private static final CommentStyle MARKDOWN = new CommentStyle(List.of("<!--"), List.of());

    private static final CommentStyle RESTRUCTURED_TEXT = new CommentStyle(List.of(), List.of(".."));

    private static final List<String> MARKDOWN_EXTENSIONS = List.of(".md", ".markdown");

    private static final List<String> RESTRUCTURED_TEXT_EXTENSIONS = List.of(".rst");

    public static boolean isValidItemName(final String name) {
        return ITEM_NAME.matcher(name).matches();
    }

    public static boolean isMarkdown(final String uriOrPath) {
        return uriOrPath != null
                && endsWithAny(uriOrPath.toLowerCase(Locale.ROOT), MARKDOWN_EXTENSIONS);
    }

    public static List<String> commentStartersFor(final String uriOrPath) {
        return styleFor(uriOrPath).starters();
    }

    public static List<String> lineStartCommentMarkersFor(final String uriOrPath) {
        return styleFor(uriOrPath).lineStartMarkers();
    }

    private static CommentStyle styleFor(final String uriOrPath) {
        if (uriOrPath == null) {
            return CODE;
        }
        final String lowerCase = uriOrPath.toLowerCase(Locale.ROOT);
        if (endsWithAny(lowerCase, MARKDOWN_EXTENSIONS)) {
            return MARKDOWN;
        }
        if (endsWithAny(lowerCase, RESTRUCTURED_TEXT_EXTENSIONS)) {
            return RESTRUCTURED_TEXT;
        }
        return CODE;
    }

    private static boolean endsWithAny(final String lowerCase, final List<String> extensions) {
        return extensions.stream().anyMatch(lowerCase::endsWith);
    }

    private OftSyntax() {
    }
}
