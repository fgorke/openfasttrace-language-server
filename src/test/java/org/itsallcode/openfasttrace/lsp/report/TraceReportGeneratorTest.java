package org.itsallcode.openfasttrace.lsp.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TraceReportGeneratorTest {

    @TempDir
    Path workspace;

    private OftWorkspaceIndex index;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(workspace.resolve("spec.md"), String.join("\n",
                "# Covered", "", "`req~covered~1`", "", "Needs: impl", "",
                "# Uncovered", "", "`req~uncovered~1`", "", "Needs: impl, utest", ""));
        Files.writeString(workspace.resolve("Login.java"), "// [impl->req~covered~1]\n");
        index = new WorkspaceIndexer().buildIndex(workspace);
    }

    private String reportFor(final TraceReportPreset preset) throws Exception {
        final Path file = new TraceReportGenerator().generate(index.allLinkedItems(), preset);
        assertThat(file).exists();
        assertThat(file.getFileName().toString()).endsWith(preset.fileExtension());
        return Files.readString(file);
    }

    // [itest->req~trace-report-on-request~1]
    @Test
    void testGivenHtmlPresetWhenGeneratingThenAnHtmlDocumentIsWritten() throws Exception {
        // when
        final String report = reportFor(TraceReportPreset.HTML);

        // then
        assertThat(report).startsWith("<!DOCTYPE html>").contains("req~uncovered~1");
    }

    // [itest->req~trace-report-on-request~1]
    @Test
    void testGivenPlainPresetWhenGeneratingThenTheDefectIsListed() throws Exception {
        // when
        final String report = reportFor(TraceReportPreset.PLAIN_FAILURES);

        // then
        assertThat(report).contains("req~uncovered~1").doesNotContain("<html>");
    }

    // [itest->req~trace-report-on-request~1]
    @Test
    void testGivenDifferentPlainPresetsWhenGeneratingThenTheirOutputDiffers() throws Exception {
        // when
        final String all = reportFor(TraceReportPreset.PLAIN_ALL);
        final String failures = reportFor(TraceReportPreset.PLAIN_FAILURES);
        final String summary = reportFor(TraceReportPreset.PLAIN_SUMMARY);

        // then:
        assertThat(all).contains("req~covered~1");
        assertThat(failures).doesNotContain("req~covered~1").contains("req~uncovered~1");
        assertThat(summary).hasLineCount(1);
    }

    // [utest->req~trace-report-on-request~1]
    @Test
    void testGivenAnUnknownIdWhenLookingUpAPresetThenNothingIsFound() {
        assertThat(TraceReportPreset.byId("bogus")).isEmpty();
        assertThat(TraceReportPreset.byId(null)).isEmpty();
    }
}
