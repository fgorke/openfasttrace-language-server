package org.itsallcode.openfasttrace.lsp;

import java.util.UUID;

import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.WorkDoneProgressBegin;
import org.eclipse.lsp4j.WorkDoneProgressCreateParams;
import org.eclipse.lsp4j.WorkDoneProgressEnd;
import org.eclipse.lsp4j.WorkDoneProgressNotification;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.tinylog.Logger;

// [impl->req~index-on-startup~3]
final class ProgressReport {

    private final LanguageClient client;
    private final String token;

    private ProgressReport(final LanguageClient client, final String token) {
        this.client = client;
        this.token = token;
    }

    static ProgressReport start(final LanguageClient client, final boolean supported,
            final String title) {
        if (client == null || !supported) {
            return new ProgressReport(null, null);
        }
        final String token = "oft-" + UUID.randomUUID();
        try {
            client.createProgress(new WorkDoneProgressCreateParams(Either.forLeft(token)));
        } catch (final RuntimeException exception) {
            Logger.debug("Client refused a progress token: " + exception.getMessage());
            return new ProgressReport(null, null);
        }
        final var begin = new WorkDoneProgressBegin();
        begin.setTitle(title);
        final ProgressReport report = new ProgressReport(client, token);
        report.notifyProgress(begin);
        return report;
    }

    void finish() {
        if (client != null) {
            notifyProgress(new WorkDoneProgressEnd());
        }
    }

    private void notifyProgress(final WorkDoneProgressNotification notification) {
        client.notifyProgress(new ProgressParams(Either.forLeft(token),
                Either.forLeft(notification)));
    }
}
