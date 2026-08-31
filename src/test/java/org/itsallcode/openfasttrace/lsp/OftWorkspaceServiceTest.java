package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.junit.jupiter.api.Test;

class OftWorkspaceServiceTest {

    private static DidChangeWatchedFilesParams changeOf(final String path) {
        final String uri = Path.of(path).toUri().toString();
        return new DidChangeWatchedFilesParams(
                List.of(new FileEvent(uri, FileChangeType.Changed)));
    }

    private static final DidChangeWatchedFilesParams SOME_CHANGE = changeOf("workspace/spec.md");

    // [utest->req~index-refresh-on-file-change~1]
    @Test
    void testGivenWatchedFileChangeWhenDebounceElapsesThenCallbackIsTriggered() throws InterruptedException {
        // given
        final OftWorkspaceService service = new OftWorkspaceService();
        final CountDownLatch latch = new CountDownLatch(1);
        service.setOnFilesChangedCallback(latch::countDown);

        // when
        service.didChangeWatchedFiles(SOME_CHANGE);

        // then
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    // [utest->req~index-refresh-on-file-change~1]
    @Test
    void testGivenRapidWatchedFileChangesWhenDebounceElapsesThenCallbackIsTriggeredOnlyOnce()
            throws InterruptedException {
        // given
        final OftWorkspaceService service = new OftWorkspaceService();
        final AtomicInteger callbackCount = new AtomicInteger();
        final CountDownLatch firstCall = new CountDownLatch(1);
        final CountDownLatch secondCall = new CountDownLatch(2);
        service.setOnFilesChangedCallback(() -> {
            callbackCount.incrementAndGet();
            firstCall.countDown();
            secondCall.countDown();
        });

        // when
        for (int i = 0; i < 5; i++) {
            service.didChangeWatchedFiles(SOME_CHANGE);
        }

        // then
        assertThat(firstCall.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(secondCall.await(200, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(callbackCount.get()).isEqualTo(1);
    }

    // [utest->req~index-refresh-on-file-change~1]
    @Test
    void testGivenAChangeToAFileNoImporterReadsWhenNotifiedThenNoRefreshIsScheduled()
            throws InterruptedException {
        // given
        final OftWorkspaceService service = new OftWorkspaceService();
        final CountDownLatch latch = new CountDownLatch(1);
        service.setOnFilesChangedCallback(latch::countDown);

        // when
        service.didChangeWatchedFiles(changeOf("workspace/target/classes/Some.class"));

        // then
        assertThat(latch.await(500, TimeUnit.MILLISECONDS)).isFalse();
    }

    // [utest->req~index-refresh-on-file-change~1]
    @Test
    void testGivenAChangeToTheIgnoreFileWhenNotifiedThenARefreshIsScheduled()
            throws InterruptedException {
        // given
        final OftWorkspaceService service = new OftWorkspaceService();
        final CountDownLatch latch = new CountDownLatch(1);
        service.setOnFilesChangedCallback(latch::countDown);

        // when
        service.didChangeWatchedFiles(changeOf("workspace/.oftignore"));

        // then
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

}
