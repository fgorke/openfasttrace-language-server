package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.itsallcode.openfasttrace.lsp.index.LocationConverter;
import org.itsallcode.openfasttrace.lsp.index.WorkspaceIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OftTextDocumentServiceProjectDiagnosticsTest {

    private static final class RecordingClient implements LanguageClient {
        private final List<PublishDiagnosticsParams> published = new ArrayList<>();

        @Override
        public void publishDiagnostics(final PublishDiagnosticsParams params) {
            published.add(params);
        }

        @Override
        public void telemetryEvent(final Object object) {
        }

        @Override
        public void showMessage(final MessageParams messageParams) {
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(
                final ShowMessageRequestParams params) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void logMessage(final MessageParams message) {
        }
    }

    @TempDir
    Path workspace;

    private OftTextDocumentService service;
    private RecordingClient client;
    private Path spec;

    @BeforeEach
    void setUp() {
        service = new OftTextDocumentService();
        client = new RecordingClient();
        service.connect(client);
        spec = workspace.resolve("spec.md");
    }

    private List<PublishDiagnosticsParams> publishedForSpec() {
        final String key = LocationConverter.toFileKey(spec.toUri().toString());
        return client.published.stream()
                .filter(params -> LocationConverter.toFileKey(params.getUri()).equals(key))
                .toList();
    }

    // [itest->req~diagnostic-trace-defects~3]
    @Test
    void testGivenDefectInAClosedFileWhenTheIndexIsUpdatedThenItsDiagnosticsArePublished()
            throws Exception {
        // given
        Files.writeString(spec, "# Login\n\n`req~login~1`\n\nNeeds: impl\n");

        // when
        service.updateIndex(new WorkspaceIndexer().buildIndex(workspace));

        // then
        assertThat(publishedForSpec()).singleElement()
                .satisfies(params -> assertThat(params.getDiagnostics()).isNotEmpty());
    }

    // [itest->req~diagnostic-trace-defects~3]
    @Test
    void testGivenAResolvedDefectWhenTheIndexIsUpdatedThenTheStaleDiagnosticsAreCleared()
            throws Exception {
        // given
        Files.writeString(spec, "# Login\n\n`req~login~1`\n\nNeeds: impl\n");
        service.updateIndex(new WorkspaceIndexer().buildIndex(workspace));

        // when
        Files.writeString(workspace.resolve("Login.java"), "// [impl->req~login~1]\n");
        service.updateIndex(new WorkspaceIndexer().buildIndex(workspace));

        // then
        final List<PublishDiagnosticsParams> updates = publishedForSpec();
        assertThat(updates).hasSize(2);
        assertThat(updates.get(1).getDiagnostics()).isEmpty();
    }
}
