package org.itsallcode.openfasttrace.lsp.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftSupportedFilesTest {

    @TempDir
    Path workspace;

    private boolean supports(final String fileName) throws IOException {
        final Path file = workspace.resolve(fileName);
        Files.writeString(file, "content\n");
        return OftSupportedFiles.isSupported(file);
    }

    // [itest->req~supported-files-from-oft~1]
    @Test
    void testGivenFilesOfEveryImportedKindWhenAskingThenAllAreSupported() throws IOException {
        assertThat(supports("Login.java")).as("tag importer").isTrue();
        assertThat(supports("spec.md")).as("markdown importer").isTrue();
        assertThat(supports("spec.rst")).as("restructuredtext importer").isTrue();
        assertThat(supports("login.feature")).as("gherkin importer").isTrue();
    }

    // [itest->req~supported-files-from-oft~1]
    @Test
    void testGivenFilesNoImporterReadsWhenAskingThenTheyAreNotSupported() throws IOException {
        assertThat(supports("notes.txt")).isFalse();
        assertThat(supports("logo.png")).isFalse();
    }
}
