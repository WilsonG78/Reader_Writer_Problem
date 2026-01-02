package org.agh;

import static java.lang.Math.abs;

public class Reader extends Thread{

    private Library library;
    private int timeStamp;
    public Reader(Library library,int sleepTime){
        this.library = library;
        if (sleepTime > 3000){
            sleepTime = 3000;
        }
        this.timeStamp = abs(sleepTime);
    }




    @Override
    public void run(){
        while (true) {
            try {
                library.startReading();
                sleep(timeStamp);
                library.stopReading();
            } catch (InterruptedException e) {
                System.out.println("Reader not allowed: " + Thread.currentThread().getName());
            }
        }
    }
}
