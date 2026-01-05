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

    public Library() {
        semaphore = new Semaphore(5, true);
    }

    public void startReading() throws InterruptedException {
        String currentThreadName = Thread.currentThread().getName();
        lock.lock();
        try{
            waitingReaders++;
            printStatus(currentThreadName+ " WAIT");
            waitingQueue.addFirst(currentThreadName);
        }finally {
            lock.unlock();
        }
        semaphore.acquire();

        lock.lock();
        try{
            waitingQueue.removeLast();
            inLibrary.add(currentThreadName);
            waitingReaders--;
            activeReaders++;
            printStatus(currentThreadName+ " ENTER");
        }finally {
            lock.unlock();
        }
    }

    public void startWriting() throws InterruptedException {
        String currentThreadName = Thread.currentThread().getName();
        lock.lock();
        try{
            waitingWriters++;
            printStatus(currentThreadName + " WAIT");
            waitingQueue.addFirst(currentThreadName);
        }finally {
            lock.unlock();
        }
        semaphore.acquire(5);

        lock.lock();
        try{
            waitingQueue.removeLast();
            inLibrary.addFirst(currentThreadName);
            waitingWriters--;
            activeWriters++;
            printStatus(currentThreadName +  " ENTER");
        }finally {
            lock.unlock();
        }
    }

    public void stopReading() {
        String currentThreadName = Thread.currentThread().getName();

        lock.lock();
        try{
            inLibrary.removeLast();
            activeReaders--;
            printStatus(currentThreadName + " LEAVE");
        }finally {
            lock.unlock();
        }

        semaphore.release();
    }

    public void stopWriting() {
        String currentThreadName = Thread.currentThread().getName();
        lock.lock();
        try{
            inLibrary.removeLast();
            activeWriters--;
            printStatus(currentThreadName+ " LEAVE");
        }finally {
            lock.unlock();
        }
        semaphore.release(5);
    }

    public void run(int numberOfWriters, int numberOfReaders, int sleepTime) {
        ArrayList<Thread> listOfPeople = new ArrayList<>();
        for (int i = 0; i < numberOfWriters; i++) {
            Writer writerTask =  new Writer(this, sleepTime);
            Thread vThread = Thread.ofVirtual().name("WRITER " + (i+1)).unstarted(writerTask);
            listOfPeople.add(vThread);

        }
        for (int i = 0; i < numberOfReaders; i++) {
            Reader readerTask = new Reader(this,sleepTime);
            Thread vThread = Thread.ofVirtual().name("READER " + (i+1)).unstarted(readerTask);
            listOfPeople.add(vThread);
        }
        Collections.shuffle(listOfPeople);
        for (Thread t : listOfPeople) {
            t.start();
        }
    }

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

    public static void main(String[] args) {
        Library lib = new Library();
        lib.run(3, 10, 1000);
    }

    public int getActiveUsersCount() {
        return activeReaders + activeWriters;
    }
    public int getWaitingQueueCount(){
        return waitingQueue.size();
    }

    public boolean isWriterInside() {
        return activeWriters != 0;
    }
}
