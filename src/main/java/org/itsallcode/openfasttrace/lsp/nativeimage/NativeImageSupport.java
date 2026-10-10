package org.itsallcode.openfasttrace.lsp.nativeimage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.services.LanguageClient;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeProxyCreation;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

/**
 * Tells GraalVM Native Image what the server reaches through reflection at run time.
 * <p>
 * LSP4J serializes every protocol type with Gson, which reads fields and calls constructors
 * reflectively, and it ships no metadata for Native Image. tinylog instantiates its writers by
 * class name. Rather than maintaining a recorded list that silently misses whatever a session did
 * not exercise, this feature registers every class of those libraries and of the server itself
 * while the image is built, so a newer LSP4J release with more types keeps working.
 * <p>
 * Gson falls back to {@code Unsafe} allocation for types without a no-arg constructor, which a
 * native image refuses. In LSP4J 1.0.0 that concerns five protocol types: the diagnostic report
 * wrappers and {@code InlineValue} come with their own JSON adapters, and
 * {@code TypeHierarchyItem} gets an {@code InstanceCreator} in {@code ServerLauncher}.
 * <p>
 * The feature is activated through {@code META-INF/native-image/.../native-image.properties},
 * so it also applies when someone runs {@code native-image -jar} on the standalone JAR.
 */
// [impl->adr~provide-a-native-binary-for-editors-without-a-java-runtime~1]
public final class NativeImageSupport implements Feature {
    private static final List<String> REFLECTIVE_PACKAGES = List.of(
            "org.eclipse.lsp4j.",
            "org.tinylog.",
            "org.itsallcode.openfasttrace.lsp.");

    @Override
    public String getDescription() {
        return "Registers LSP4J, tinylog and server classes for reflection and the LSP client proxy";
    }

    @Override
    public void beforeAnalysis(final BeforeAnalysisAccess access) {
        // LSP4J hands the server a java.lang.reflect.Proxy for the client, see ServiceEndpoints.toServiceObject
        RuntimeProxyCreation.register(LanguageClient.class, Endpoint.class);

        for (final Path entry : access.getApplicationClassPath()) {
            forEachClassName(entry, className -> {
                if (isReflective(className)) {
                    registerForReflection(access.findClassByName(className));
                }
            });
        }
    }

    private static boolean isReflective(final String className) {
        return REFLECTIVE_PACKAGES.stream().anyMatch(className::startsWith);
    }

    private static void registerForReflection(final Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        try {
            // The bulk registerAllDeclared*() variants only make the members visible to getDeclared*()
            // queries. Gson and LSP4J have to invoke them, so register the members themselves.
            RuntimeReflection.register(clazz);
            RuntimeReflection.register(clazz.getDeclaredConstructors());
            RuntimeReflection.register(clazz.getDeclaredMethods());
            RuntimeReflection.register(clazz.getDeclaredFields());
        } catch (final LinkageError e) {
            // Optional dependency missing, e.g. LSP4J's websocket classes without a websocket API on the classpath.
        }
    }

    private static void forEachClassName(final Path entry, final Consumer<String> consumer) {
        if (Files.isDirectory(entry)) {
            forEachClassInDirectory(entry, consumer);
        } else if (entry.toString().endsWith(".jar")) {
            forEachClassInJar(entry, consumer);
        }
    }

    private static void forEachClassInDirectory(final Path directory, final Consumer<String> consumer) {
        try (Stream<Path> files = Files.walk(directory)) {
            files.map(file -> directory.relativize(file).toString().replace(directory.getFileSystem().getSeparator(), "/"))
                    .forEach(relativePath -> acceptClassFile(relativePath, consumer));
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to list classes in " + directory, e);
        }
    }

    private static void forEachClassInJar(final Path jar, final Consumer<String> consumer) {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            jarFile.stream().forEach(jarEntry -> acceptClassFile(jarEntry.getName(), consumer));
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to list classes in " + jar, e);
        }
    }

    private static void acceptClassFile(final String path, final Consumer<String> consumer) {
        if (!path.endsWith(".class") || path.endsWith("module-info.class") || path.endsWith("package-info.class")) {
            return;
        }
        consumer.accept(path.substring(0, path.length() - ".class".length()).replace('/', '.'));
    }
}
