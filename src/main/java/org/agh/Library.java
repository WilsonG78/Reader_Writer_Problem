package org.agh;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;


public class Library {

    private final Semaphore semaphore;
    private int waitingReaders = 0;
    private int waitingWriters = 0;
    private int activeReaders = 0;
    private int activeWriters = 0;

    public Library() {
        semaphore = new Semaphore(5, true);
    }

    public void startReading() throws InterruptedException {
        synchronized (this) {
            waitingReaders++;
            printStatus("READER " + Thread.currentThread().getName() + " WAIT");

        }
        semaphore.acquire();

        synchronized (this) {
            waitingReaders--;
            activeReaders++;
            printStatus("READER " + Thread.currentThread().getName() + " ENTER");
        }
    }

    public void startWriting() throws InterruptedException {
        synchronized (this) {
            waitingWriters++;
            printStatus("WRITER" + Thread.currentThread().getName() + " WAIT");

        }
        semaphore.acquire(5);

        synchronized (this) {
            waitingWriters--;
            activeWriters++;
            printStatus("Writer " + Thread.currentThread().getName() + " ENTER");
        }
    }

    public void stopReading() {
        semaphore.release();
        synchronized (this) {
            activeReaders--;
            printStatus("READER " + Thread.currentThread().getName() + " LEAVE");
        }
    }

    public void stopWriting() {

        synchronized (this) {
            activeWriters--;
            printStatus("WRITER " + Thread.currentThread().getName() + " LEAVE");
        }
        semaphore.release(5);

    }

    public void run(int numberOfWriters, int numberOfReaders, int sleepTime) {
        Thread[] readers = new Thread[numberOfReaders];
        Thread[] writers = new Thread[numberOfWriters];
        for (int i = 0; i < numberOfWriters; i++) {
            writers[i] = new Writer(this, sleepTime);
        }
        for (int i = 0; i < numberOfReaders; i++) {
            readers[i] = new Reader(this, sleepTime);
        }
        for (Thread reader : readers) {
            reader.start();
        }
        for (Thread writer : writers) {
            writer.start();
        }
    }

    private void printStatus(String message) {
        System.out.printf("[WR: %d, AR: %d | WW: %d, AW: %d] -> %s%n",
                waitingReaders, activeReaders, waitingWriters, activeWriters, message);
    }

    public static void main(String[] args) {
        Library lib = new Library();
        lib.run(3, 10, 1000);
    }

}
