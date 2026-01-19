package org.agh;


import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.lang.Math.abs;

/**
 * The {@Writer} class represents a writer task executed in a separated thread.
 * <p>
 *     An instance of this class starts and stops a writing proccess
 * </p>
 * <p>
 *     This Class implements {@link Runnable} interface, which allows it
 *     to be executed by a {@link Thread}.
 * </p>
 */
@Getter
public class Writer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Writer.class);
    private final Library library;
    private final int timeStamp;
    /**
     * Creates a new {@code Writer} instance.
     *
     * @param library   the {@link Library} instance used for writing operations
     * @param sleepTime the thread sleep time in milliseconds; values greater
     *                  than 3000 ms are capped at 3000 ms
     */
    public Writer(Library library, int sleepTime) {
        this.library = library;
        if (sleepTime > 3000) {
            sleepTime = 3000;
        }
        timeStamp = abs(sleepTime);
    }

    /**
     * The entry point of the thread execution.
     * <p>
     * The thread continuously starts the writing process, sleeps for the
     * specified time, and then stops the writing process until the thread
     * is interrupted.
     * </p>
     *
     * <p>
     * If the thread is interrupted during sleep, the interruption flag
     * is restored and the error is logged.
     * </p>
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                library.startWriting();
                Thread.sleep(timeStamp);
                library.stopWriting();
            } catch (InterruptedException e) {
                logger.error("ERROR");
                Thread.currentThread().interrupt();
            }
        }
    }
}
