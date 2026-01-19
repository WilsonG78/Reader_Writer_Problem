package org.agh;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The {@code Library} class represents a shared resource accessed concurrently
 * by reader and writer threads.
 * <p>
 * The class implements a readers–writers synchronization mechanism using
 * a {@link Semaphore} and a {@link ReentrantLock}. Readers may access the library
 * concurrently, while writers require exclusive access.
 * </p>
 *
 * <p>
 * The semaphore controls the maximum number of concurrent readers and enforces
 * fairness, while the lock protects shared state such as counters and queues.
 * </p>
 */
@Getter
public class Library {

    private static final Logger logger = LoggerFactory.getLogger(Library.class);
    private final ReentrantLock lock = new ReentrantLock();
    private final Semaphore semaphore;
    private int waitingReaders = 0;
    private int waitingWriters = 0;
    private int activeReaders = 0;
    private int activeWriters = 0;
    private final List<String> waitingQueue = new LinkedList<>();
    private final List<String> inLibrary = new LinkedList<>();

    /**
     * Creates a new {@code Library} instance.
     * <p>
     * The semaphore is initialized with 5 permits and fairness enabled,
     * allowing up to 5 concurrent readers or a single writer.
     * </p>
     */
    public Library() {
        semaphore = new Semaphore(5, true);
    }

    /**
     * Starts a reading operation.
     * <p>
     * The calling thread is added to the waiting queue and waits until a
     * semaphore permit becomes available. Once acquired, the reader enters
     * the library and updates the internal state.
     * </p>
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     *                              to acquire the semaphore
     */
    public void startReading() throws InterruptedException {
        String currentThreadName = Thread.currentThread().getName();
        lock.lock();
        try {
            waitingReaders++;
            printStatus(currentThreadName + " WAIT");
            waitingQueue.addFirst(currentThreadName);
        } finally {
            lock.unlock();
        }
        semaphore.acquire();

        lock.lock();
        try {
            waitingQueue.removeLast();
            inLibrary.add(currentThreadName);
            waitingReaders--;
            activeReaders++;
            printStatus(currentThreadName + " ENTER");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Starts a writing operation.
     * <p>
     * The calling thread waits until it can acquire all semaphore permits,
     * guaranteeing exclusive access to the library.
     * </p>
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     *                              to acquire the semaphore
     */
    public void startWriting() throws InterruptedException {
        String currentThreadName = Thread.currentThread().getName();
        lock.lock();
        try {
            waitingWriters++;
            printStatus(currentThreadName + " WAIT");
            waitingQueue.addFirst(currentThreadName);
        } finally {
            lock.unlock();
        }
        semaphore.acquire(5);

        lock.lock();
        try {
            waitingQueue.removeLast();
            inLibrary.addFirst(currentThreadName);
            waitingWriters--;
            activeWriters++;
            printStatus(currentThreadName + " ENTER");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Ends a reading operation and releases the semaphore permit.
     */
    public void stopReading() {
        String currentThreadName = Thread.currentThread().getName();

        lock.lock();
        try {
            inLibrary.removeLast();
            activeReaders--;
            printStatus(currentThreadName + " LEAVE");
        } finally {
            lock.unlock();
        }

        semaphore.release();
    }

    /**
     * Ends a writing operation and releases all semaphore permits.
     */
    public void stopWriting() {
        String currentThreadName = Thread.currentThread().getName();
        lock.lock();
        try {
            inLibrary.removeLast();
            activeWriters--;
            printStatus(currentThreadName + " LEAVE");
        } finally {
            lock.unlock();
        }
        semaphore.release(5);
    }

    /**
     * Starts the simulation by creating and running reader and writer threads.
     * <p>
     * Threads are created as virtual threads and started in randomized order.
     * </p>
     *
     * @param numberOfWriters number of writer threads
     * @param numberOfReaders number of reader threads
     * @param sleepTime       sleep time for each reader and writer task
     */
    public void run(int numberOfWriters, int numberOfReaders, int sleepTime) {
        ArrayList<Thread> listOfPeople = new ArrayList<>();
        for (int i = 0; i < numberOfWriters; i++) {
            Writer writerTask = new Writer(this, sleepTime);
            Thread vThread = Thread.ofVirtual().name("WRITER " + (i + 1)).unstarted(writerTask);
            listOfPeople.add(vThread);

        }
        for (int i = 0; i < numberOfReaders; i++) {
            Reader readerTask = new Reader(this, sleepTime);
            Thread vThread = Thread.ofVirtual().name("READER " + (i + 1)).unstarted(readerTask);
            listOfPeople.add(vThread);
        }
        Collections.shuffle(listOfPeople);
        for (Thread t : listOfPeople) {
            t.start();
        }

        for (Thread t : listOfPeople) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Main thread interrupted", e);
            }
        }
    }

    /**
     * Prints the current state of the library and thread activity to the log.
     *
     * @param message action description to be logged
     */
    private void printStatus(String message) {
        if (!logger.isInfoEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        String separator = "+----------------------------------------------------------------------------------+";
        String newline = System.lineSeparator();

        sb.append(newline);
        sb.append(separator).append(newline);

        sb.append(String.format("| ACTION: %-72s |%n", message));
        sb.append(separator).append(newline);

        sb.append(String.format("| STATS:  Wait(R): %-2d | Active(R): %-2d || Wait(W): %-2d | Active(W): %-2d      |%n",
                waitingReaders, activeReaders, waitingWriters, activeWriters));

        sb.append("| DETAILED STATE:                                                                  |").append(newline);
        sb.append(String.format("| > Queue:   %-69s |%n", waitingQueue.toString()));
        sb.append(String.format("| > Library: %-69s |%n", inLibrary.toString()));
        sb.append(separator);

        logger.info(sb.toString());
    }

    /**
     * Returns the total number of active users in the library.
     *
     * @return number of active readers and writers
     */
    public int getActiveUsersCount() {
        return activeReaders + activeWriters;
    }

    /**
     * Returns the number of threads waiting to enter the library.
     *
     * @return size of the waiting queue
     */
    public int getWaitingQueueCount() {
        return waitingQueue.size();
    }

    /**
     * Checks whether a writer is currently inside the library.
     *
     * @return {@code true} if a writer is active, {@code false} otherwise
     */
    public boolean isWriterInside() {
        return activeWriters != 0;
    }

    /**
     * Application entry point used for demonstration and testing.
     *
     * @param args args[0] numberOfWriters args[1] numberOfReaders arg[2] timeStamp
     */
    public static void main(String[] args) {
        Library lib = new Library();

        try {
            lib.run(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            logger.error("Arguments must be exactly 3 integers");
        }
    }
}
