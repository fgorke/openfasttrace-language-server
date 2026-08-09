package org.itsallcode.openfasttrace.lsp.report;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.itsallcode.openfasttrace.api.report.ReportVerbosity;

// [impl->req~trace-report-on-request~1]
public enum TraceReportPreset {

    HTML("html", "html", ReportVerbosity.ALL,
            "HTML report"),
    PLAIN_ALL("plain-all", "plain", ReportVerbosity.ALL,
            "Plain text, every item"),
    PLAIN_FAILURES("plain-failures", "plain", ReportVerbosity.FAILURE_DETAILS,
            "Plain text, defects only"),
    PLAIN_DIRECT_FAILURES("plain-direct-failures", "plain", ReportVerbosity.DIRECT_FAILURE_DETAILS,
            "Plain text, defects only, without those inherited from covered items"),
    PLAIN_SUMMARY("plain-summary", "plain", ReportVerbosity.SUMMARY,
            "Plain text, one line summary");

    private final String id;
    private final String outputFormat;
    private final ReportVerbosity verbosity;
    private final String title;

    TraceReportPreset(final String id, final String outputFormat,
            final ReportVerbosity verbosity, final String title) {
        this.id = id;
        this.outputFormat = outputFormat;
        this.verbosity = verbosity;
        this.title = title;
    }

    public String id() {
        return id;
    }

    public String outputFormat() {
        return outputFormat;
    }

    public ReportVerbosity verbosity() {
        return verbosity;
    }

    public String title() {
        return title;
    }

    public String fileExtension() {
        return "html".equals(outputFormat) ? ".html" : ".txt";
    }

    public static Optional<TraceReportPreset> byId(final String id) {
        if (id == null) {
            return Optional.empty();
        }
        final String normalized = id.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(preset -> preset.id.equals(normalized))
                .findFirst();
    }

    public static List<String> ids() {
        return Arrays.stream(values()).map(TraceReportPreset::id).toList();
    }
}
