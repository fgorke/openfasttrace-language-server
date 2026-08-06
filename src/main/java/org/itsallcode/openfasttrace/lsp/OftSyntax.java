package org.itsallcode.openfasttrace.lsp;

import java.util.List;
import java.util.regex.Pattern;

import org.itsallcode.openfasttrace.api.core.SpecificationItemId;

public final class OftSyntax {

    public static final Pattern SPECIFICATION_ITEM_ID = SpecificationItemId.ID_PATTERN;

    public static final Pattern SPECIFICATION_ITEM_DEFINITION_LINE = Pattern.compile(
            "^`?(" + SPECIFICATION_ITEM_ID.pattern() + ")`?.*$");

    public static final Pattern SECTION_KEYWORD_LINE = Pattern.compile(
            "^\\s*(Needs|Covers|Depends|Status|Description|Rationale|Comment|Tags):");

    private static final String COVERED_ID_LIST = SPECIFICATION_ITEM_ID.pattern()
            + "(?:\\s*,\\s*" + SPECIFICATION_ITEM_ID.pattern() + ")*";

    public static final Pattern COVERAGE_TAG = Pattern.compile(
            "\\[\\s*\\p{Alpha}+(?:~" + SpecificationItemId.ITEM_NAME_PATTERN + "~\\d+)?\\s*->\\s*"
                    + COVERED_ID_LIST + "\\s*]");

    public static final Pattern COVERAGE_TAG_LOOSE = Pattern.compile("\\[[^\\]\\n]*->[^\\]\\n]*]");

    public static final String ID_CHARACTER_CLASS = "[\\p{L}\\p{N}~._-]";

    public static final List<String> COMMENT_MARKERS =
            List.of("//", "#", "--", ";", "/*", "<!--", "*");

    public static final List<String> COMMENT_STARTERS =
            List.of("//", "#", "--", ";", "/*", "<!--");

    private OftSyntax() {
    }
}
