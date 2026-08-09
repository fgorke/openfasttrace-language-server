package org.itsallcode.openfasttrace.lsp.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.itsallcode.openfasttrace.api.ReportSettings;
import org.itsallcode.openfasttrace.api.core.LinkedSpecificationItem;
import org.itsallcode.openfasttrace.api.core.Trace;
import org.itsallcode.openfasttrace.core.Oft;
import org.tinylog.Logger;

// [impl->req~trace-report-on-request~1]
public final class TraceReportGenerator {

    private final Oft oft;

    public TraceReportGenerator() {
        this(Oft.create());
    }

    TraceReportGenerator(final Oft oft) {
        this.oft = oft;
    }

    public Path generate(final List<LinkedSpecificationItem> linkedItems,
            final TraceReportPreset preset) throws IOException {
        final Trace trace = oft.trace(linkedItems);
        final Path file = Files.createTempFile("oft-trace-", preset.fileExtension());
        final ReportSettings settings = ReportSettings.builder()
                .outputFormat(preset.outputFormat())
                .verbosity(preset.verbosity())
                .build();
        oft.reportToPath(trace, file, settings);
        Logger.info("Trace report written to " + file + " (" + preset.id() + ", "
                + trace.countDefects() + " defect(s) of " + trace.count() + " item(s))");
        return file;
    }
}
