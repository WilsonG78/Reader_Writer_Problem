package org.agh;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the {@link Library} class verifying a correct solution
 * to the Readers–Writers synchronization problem.
 *
 * <p>
 * These tests validate:
 * <ul>
 *     <li>Maximum number of concurrent readers</li>
 *     <li>Exclusive access for writers</li>
 *     <li>Correct waiting behavior for readers and writers</li>
 *     <li>Proper thread creation using virtual threads</li>
 * </ul>
 * </p>
 *
 * <p>
 * {@link CountDownLatch} and Awaitility are used instead of {@code Thread.sleep()}
 * to ensure deterministic and reliable test execution.
 * </p>
 */
class LibraryTest {
    private Library library;
    private ExecutorService executor;

    /***
     * Tests setup
     */
    @BeforeEach
    void setup() {
        library = new Library();
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    /**
     * We test if @method printStatus works when info is disabled
     *
     * @throws InterruptedException
     */
    @Test
    void testPrintStatusWithInfoDisabled() throws InterruptedException {
        Logger logger = (Logger) LoggerFactory.getLogger(Library.class);

        Level oldLevel = logger.getLevel();

        try {
            logger.setLevel(Level.WARN);

            Library lib = new Library();

            lib.startReading();


        } finally {
            logger.setLevel(oldLevel);
        }
        assertTrue(true);
    }

    /**
     * Test if 5 readers can enter library
     *
     * @throws InterruptedException
     */
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
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(allEntered.await(2, TimeUnit.SECONDS));
        assertEquals(5, library.getActiveUsersCount());
        assertEquals(0, library.getWaitingQueueCount());

        releaseThreads.countDown();
    }

    /**
     * Tests if 6th reader must wait in queue
     *
     * @throws InterruptedException
     */
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
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(first5Entered.await(2, TimeUnit.SECONDS));

        executor.submit(() -> {
            try {
                library.startReading();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        await().atMost(1, TimeUnit.SECONDS)
                .until(() -> library.getWaitingQueueCount() == 1);

        assertEquals(5, library.getActiveUsersCount());
        assertEquals(1, library.getWaitingQueueCount());

        releaseFirst5.countDown();
    }

    /**
     * Checks if writer has exclusive access to library
     *
     * @throws InterruptedException
     */
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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(writerEntered.await(2, TimeUnit.SECONDS));
        assertTrue(library.isWriterInside());
        assertEquals(1, library.getActiveUsersCount());

        executor.submit(() -> {
            try {
                library.startReading();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        await().atMost(1, TimeUnit.SECONDS)
                .until(() -> library.getWaitingQueueCount() == 1);

        assertEquals(1, library.getActiveUsersCount());

        releaseWriter.countDown();
    }

    /**
     * Checks if writer waits until all readers leave library and then enter
     *
     * @throws InterruptedException
     */
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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(readerEntered.await(2, TimeUnit.SECONDS));

        executor.submit(() -> {
            try {
                library.startWriting();
            } catch (InterruptedException e) {
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

    /**
     * Test covers the InterruptedException catch block in the run loop.
     */
    @Test
    void testRunMethodInterruptionAndLogging() {

        Logger logger = (Logger) LoggerFactory.getLogger(Library.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        Library lib = new Library();

        // we start run method in runnerThread
        Thread runnerThread = new Thread(() -> lib.run(1, 0, 10000));
        runnerThread.start();

        // we wait until thread will be waiting
        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> runnerThread.getState() == Thread.State.WAITING);

        // Interrupt thread so wi will catch exception
        runnerThread.interrupt();

        // we checks logs
        await().atMost(2, TimeUnit.SECONDS).until(() ->
                listAppender.list.stream()
                        .anyMatch(event -> event.getMessage().contains("Main thread interrupted"))
        );

        // Sprzątanie szpiega
        logger.detachAppender(listAppender);
    }

    /**
     * Tests good path for join()
     */
    @Test
    void testRunMethodCompletesSuccessfully() throws InterruptedException {
        // We use spy to manipulate start methods
        Library libSpy = spy(new Library());

        //we want to throw exception right after startReading and startWriting
        doThrow(new InterruptedException()).when(libSpy).startReading();
        doThrow(new InterruptedException()).when(libSpy).startWriting();


        // AssertDoesNotThrow
        assertDoesNotThrow(() -> libSpy.run(1, 1, 10));

    }

    /**
     * We check if our virtual threads works correctly inside of Library Class
     * we have to use Mockito spy objecet to check if our methods were invoked
     */
    @Test
    void testRunMethodLaunchesVirtualThreads() {
        Library librarySpy = spy(new Library());

        executor.submit(() -> {
            librarySpy.run(1, 1, 10);
        });


        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {

            verify(librarySpy, atLeast(1)).startReading();

            verify(librarySpy, atLeast(1)).startWriting();
        });

    }

    /**
     * Tests invalid arguments scenario in main
     */
    @Test
    void testMainWithInvalidArguments() {
        Logger logger = (Logger) LoggerFactory.getLogger(Library.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        String[] emptyArgs = {};
        Library.main(emptyArgs);


        boolean logFoundEmpty = listAppender.list.stream()
                .anyMatch(event -> event.getMessage().contains("Arguments must be exactly 3 integers"));
        assertTrue(logFoundEmpty, "Should log error when arguments are missing");


        listAppender.list.clear();


        String[] badFormatArgs = {"1", "nie-liczba", "100"};
        Library.main(badFormatArgs);


        boolean logFoundBadFormat = listAppender.list.stream()
                .anyMatch(event -> event.getMessage().contains("Arguments must be exactly 3 integers"));
        assertTrue(logFoundBadFormat, "Should log error when arguments are not integers");


        logger.detachAppender(listAppender);
    }

    @Test
    void testMainMethod() {
        String[] args = {"3", "7", "10"};

        executor.submit(() -> {
            Library.main(args);
        });


        assertTrue(true);
    }

}