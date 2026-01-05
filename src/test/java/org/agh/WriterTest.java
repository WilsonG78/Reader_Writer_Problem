package org.agh;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

class WriterTest {

    private Library library;
    private Writer writer;

    @BeforeEach
    void setup(){
        library = new Library();
    }
    @Test
    void under3000SleepTimeStamp(){
        writer = new Writer(library,4000);
        assertEquals(3000,writer.getTimeStamp());
    }

    @Test
    void absoluteValueTimeStamp(){
        writer = new Writer(library,-1200);
        assertEquals(1200, writer.getTimeStamp());
    }

    @Test
    void setLibraryTest(){
        writer = new Writer(library,10);
        assertEquals(library,writer.getLibrary());
    }
}
