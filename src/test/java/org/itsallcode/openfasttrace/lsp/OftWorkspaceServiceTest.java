package org.itsallcode.openfasttrace.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileEvent;
import org.junit.jupiter.api.Test;

class OftWorkspaceServiceTest {

    private static final DidChangeWatchedFilesParams SOME_CHANGE =
            new DidChangeWatchedFilesParams(List.of(new FileEvent()));

    // [utest->req~index-refresh-on-save~2]
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

    // [utest->req~index-refresh-on-save~2]
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

}
