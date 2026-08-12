package org.itsallcode.openfasttrace.lsp.index;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.tinylog.Logger;

// [impl->req~index-ignore-file~1]
public final class OftIgnore {

    public static final String FILE_NAME = ".oftignore";

    // [impl->req~index-on-startup~2]
    private static final List<String> DEFAULT_PATTERNS = List.of(
            "{target,build,out,dist,node_modules}",
            "**/{target,build,out,dist,node_modules}",
            ".*",
            "**/.*");

    private final Path workspaceRoot;
    private final List<PathMatcher> matchers;

    private OftIgnore(final Path workspaceRoot, final List<PathMatcher> matchers) {
        this.workspaceRoot = workspaceRoot;
        this.matchers = matchers;
    }

    public static OftIgnore none() {
        return new OftIgnore(null, List.of());
    }

    public static OftIgnore load(final Path workspaceRoot) {
        final List<PathMatcher> matchers = new ArrayList<>(compileAll(DEFAULT_PATTERNS));
        matchers.addAll(compileAll(patternsFrom(workspaceRoot.resolve(FILE_NAME))));
        return new OftIgnore(workspaceRoot.toAbsolutePath().normalize(), matchers);
    }

    private static List<String> patternsFrom(final Path file) {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            final List<String> patterns = Files.readAllLines(file).stream()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.endsWith("/") ? line.substring(0, line.length() - 1) : line)
                    .toList();
            Logger.info("Loaded " + patterns.size() + " pattern(s) from " + file);
            return patterns;
        } catch (final IOException exception) {
            Logger.warn("Could not read " + file + ": " + exception.getMessage());
            return List.of();
        }
    }

    private static List<PathMatcher> compileAll(final List<String> patterns) {
        return patterns.stream().map(OftIgnore::compile).flatMap(Optional::stream).toList();
    }

    private static Optional<PathMatcher> compile(final String pattern) {
        try {
            return Optional.of(FileSystems.getDefault().getPathMatcher("glob:" + pattern));
        } catch (final RuntimeException exception) {
            Logger.warn("Ignoring invalid pattern in " + FILE_NAME + ": " + pattern);
            return Optional.empty();
        }
    }

    public boolean isExcluded(final Path path) {
        if (matchers.isEmpty()) {
            return false;
        }
        final Path relative = relativize(path);
        if (relative == null) {
            return false;
        }
        for (Path candidate = relative; candidate != null; candidate = candidate.getParent()) {
            final Path current = candidate;
            if (matchers.stream().anyMatch(matcher -> matcher.matches(current))) {
                return true;
            }
        }
        return false;
    }

    public boolean isExcluded(final String uriOrPath) {
        try {
            final Path path = uriOrPath.startsWith("file:")
                    ? Path.of(URI.create(uriOrPath))
                    : Path.of(uriOrPath);
            return isExcluded(path);
        } catch (final RuntimeException exception) {
            return false;
        }
    }

    private Path relativize(final Path path) {
        try {
            final Path relative = workspaceRoot.relativize(path.toAbsolutePath().normalize());
            return relative.startsWith("..") ? null : relative;
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }
}
