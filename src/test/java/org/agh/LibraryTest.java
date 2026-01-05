package org.agh;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the Library class verifying the Readers-Writers problem solution.
 * These tests ensure mutual exclusion for writers, shared access for readers,
 * and correct queuing behavior using CountDownLatch for deterministic synchronization
 * to avoid flaky tests associated with Thread.sleep().
 */
class LibraryTest {
    private Library library;
    private ExecutorService executor;

    @BeforeEach
    void setup() {
        library = new Library();
        executor = Executors.newCachedThreadPool();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testMax5ReadersCanEnter() throws InterruptedException {
        int readersCount = 5;
        CountDownLatch allEntered = new CountDownLatch(readersCount);
        CountDownLatch releaseThreads = new CountDownLatch(1);

        for (int i = 0; i < readersCount; i++) {
            executor.submit(() -> {
                try {
                    library.startReading();
                    allEntered.countDown();
                    releaseThreads.await();
                    library.stopReading();
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(allEntered.await(2, TimeUnit.SECONDS));
        assertEquals(5, library.getActiveUsersCount());
        assertEquals(0, library.getWaitingQueueCount());

        releaseThreads.countDown();
    }

    @Test
    void test6thReaderMustWait() throws InterruptedException {
        CountDownLatch first5Entered = new CountDownLatch(5);
        CountDownLatch releaseFirst5 = new CountDownLatch(1);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    library.startReading();
                    first5Entered.countDown();
                    releaseFirst5.await();
                    library.stopReading();
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(first5Entered.await(2, TimeUnit.SECONDS));

        executor.submit(() -> {
            try {
                library.startReading();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        });

        await().atMost(1, TimeUnit.SECONDS)
                .until(() -> library.getWaitingQueueCount() == 1);

        assertEquals(5, library.getActiveUsersCount());
        assertEquals(1, library.getWaitingQueueCount());

        releaseFirst5.countDown();
    }

    @Test
    void writerHasExclusiveAccess() throws InterruptedException {
        CountDownLatch writerEntered = new CountDownLatch(1);
        CountDownLatch releaseWriter = new CountDownLatch(1);

        executor.submit(() -> {
            try {
                library.startWriting();
                writerEntered.countDown();
                releaseWriter.await();
                library.stopWriting();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(writerEntered.await(2, TimeUnit.SECONDS));
        assertTrue(library.isWriterInside());
        assertEquals(1, library.getActiveUsersCount());

        executor.submit(() -> {
            try {
                library.startReading();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        });

        await().atMost(1, TimeUnit.SECONDS)
                .until(() -> library.getWaitingQueueCount() == 1);

        assertEquals(1, library.getActiveUsersCount());

        releaseWriter.countDown();
    }

    @Test
    void testWriterWaitsForReaders() throws InterruptedException {
        CountDownLatch readerEntered = new CountDownLatch(1);
        CountDownLatch releaseReader = new CountDownLatch(1);

        executor.submit(() -> {
            try {
                library.startReading();
                readerEntered.countDown();
                releaseReader.await();
                library.stopReading();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(readerEntered.await(2, TimeUnit.SECONDS));

        executor.submit(() -> {
            try {
                library.startWriting();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        });

        await().atMost(1, TimeUnit.SECONDS)
                .until(() -> library.getWaitingQueueCount() == 1);

        assertFalse(library.isWriterInside());

        releaseReader.countDown();

        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> library.isWriterInside());

        assertTrue(library.isWriterInside());
    }
}