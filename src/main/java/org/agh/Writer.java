package org.agh;


import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.abs;
@Getter
public class Writer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Writer.class);
    private final Library library;
    private final int timeStamp;
    public Writer(Library library, int sleepTime) {
        this.library = library;
        if (sleepTime > 3000) {
            sleepTime = 3000;
        }
        timeStamp = abs(sleepTime);
    }


    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                library.startWriting();
                Thread.sleep(timeStamp);
                library.stopWriting();
            } catch (InterruptedException _) {
                logger.error("ERROR");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
