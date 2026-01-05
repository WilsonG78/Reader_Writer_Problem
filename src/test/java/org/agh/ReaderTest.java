package org.agh;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReaderTest {

    private Library library;
    private Reader reader;

    @BeforeEach
    void setup(){
        library = new Library();
    }
    @Test
    void under3000SleepTimeStamp(){
        reader = new Reader(library,4000);
        assertEquals(3000,reader.getTimeStamp());
    }

    @Test
    void absoluteValueTimeStamp(){
        reader = new Reader(library,-1200);
        assertEquals(1200, reader.getTimeStamp());
    }

    @Test
    void setLibraryTest(){
        reader = new Reader(library,10);
        assertEquals(library,reader.getLibrary());
    }
}
