package org.agh;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReaderTest {

    private Library library;
    private Reader reader;

    @BeforeEach
    void setup() {
        library = new Library();
    }

    /**
     * Test checks if upper bound 3000ms works correctly
     */
    @Test
    void under3000SleepTimeStamp() {
        reader = new Reader(library, 4000);
        assertEquals(3000, reader.getTimeStamp());
    }

    @Test
    void absoluteValueTimeStamp() {
        reader = new Reader(library, -1200);
        assertEquals(1200, reader.getTimeStamp());
    }

    @Test
    void setLibraryTest() {
        reader = new Reader(library, 10);
        assertEquals(library, reader.getLibrary());
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

        Reader newReader = new Reader(libraryMock, 1);

        Thread readerThread = new Thread(newReader::run);
        readerThread.start();

        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> Mockito.verify(libraryMock, Mockito.atLeastOnce()).startReading());

        readerThread.interrupt();

        readerThread.join(1000);


        assertFalse(readerThread.isAlive(), "Thread should die");

    }
}
