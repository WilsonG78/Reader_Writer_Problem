package org.agh;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WriterTest {

    private Library library;
    private Writer writer;

    @BeforeEach
    void setup() {
        library = new Library();
    }

    /**
     * Test checks if upper bound 3000ms works correctly
     */
    @Test
    void under3000SleepTimeStamp() {
        writer = new Writer(library, 4000);
        assertEquals(3000, writer.getTimeStamp());
    }

    @Test
    void absoluteValueTimeStamp() {
        writer = new Writer(library, -1200);
        assertEquals(1200, writer.getTimeStamp());
    }

    @Test
    void setLibraryTest() {
        writer = new Writer(library, 10);
        assertEquals(library, writer.getLibrary());
    }

    /**
     * We check if run method works correctly
     * especially we check if startWriting method is invoked inside run
     *
     * @throws InterruptedException
     */
    @Test
    void testRun() throws InterruptedException {
        Library libraryMock = Mockito.mock(Library.class);

        Writer newWriter = new Writer(libraryMock, 1);

        Thread readerThread = new Thread(newWriter::run);
        readerThread.start();

        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> Mockito.verify(libraryMock, Mockito.atLeastOnce()).startWriting());

        readerThread.interrupt();

        readerThread.join(1000);


        assertFalse(readerThread.isAlive(), "Thread should die");

    }
}
