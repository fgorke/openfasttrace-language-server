package org.itsallcode.openfasttrace.lsp.highlighting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.itsallcode.openfasttrace.lsp.OftSyntax;

public final class OftSemanticTokensProvider {

    public static final List<String> TOKEN_TYPES = List.of("type", "keyword", "macro");

    private static final int TYPE_SPECIFICATION_ITEM = 0;
    private static final int TYPE_KEYWORD = 1;
    private static final int TYPE_COVERAGE_TAG = 2;

    private OftSemanticTokensProvider() {
    }

    public static List<Integer> computeTokens(final List<String> lines) {
        final List<int[]> tokens = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            final String line = lines.get(lineIndex);
            collectMatches(tokens, lineIndex, line, OftSyntax.SPECIFICATION_ITEM_DEFINITION_LINE, 1,
                    TYPE_SPECIFICATION_ITEM);
            collectMatches(tokens, lineIndex, line, OftSyntax.SECTION_KEYWORD_LINE, 1, TYPE_KEYWORD);
            collectMatches(tokens, lineIndex, line, OftSyntax.COVERAGE_TAG, 0, TYPE_COVERAGE_TAG);
        }
        tokens.sort(Comparator.<int[]>comparingInt(t -> t[0]).thenComparingInt(t -> t[1]));
        return encode(tokens);
    }

    private static void collectMatches(final List<int[]> tokens, final int lineIndex, final String line,
            final Pattern pattern, final int group, final int tokenType) {
        final Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            final int start = matcher.start(group);
            final int length = matcher.end(group) - start;
            tokens.add(new int[] { lineIndex, start, length, tokenType });
        }
    }

    private static List<Integer> encode(final List<int[]> tokens) {
        final List<Integer> data = new ArrayList<>();
        int previousLine = 0;
        int previousStart = 0;
        for (final int[] token : tokens) {
            final int line = token[0];
            final int start = token[1];
            final int deltaLine = line - previousLine;
            final int deltaStart = deltaLine == 0 ? start - previousStart : start;
            data.add(deltaLine);
            data.add(deltaStart);
            data.add(token[2]);
            data.add(token[3]);
            data.add(0);
            previousLine = line;
            previousStart = start;
        }
        return data;
    }
}
