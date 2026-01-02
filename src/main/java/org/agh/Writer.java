package org.agh;


public class Writer extends Thread {

    private Library library;
    private int timeStamp;

    public Writer(Library library, int sleepTime) {
        this.library = library;
        timeStamp = sleepTime;
    }


    @Override
    public void run() {
        while (true) {
            try {
                library.startWriting();
                sleep(timeStamp);
                library.stopWriting();
            } catch (InterruptedException e) {
                System.out.println("Writer not allowed: " + Thread.currentThread().getName());
            }
        }
    }
}
