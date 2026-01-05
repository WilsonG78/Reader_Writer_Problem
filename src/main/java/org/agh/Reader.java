package org.agh;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.abs;
@Getter
public class Reader implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(Reader.class);
    private final Library library;
    private final int timeStamp;
    public Reader(Library library,int sleepTime){
        this.library = library;
        if (sleepTime > 3000) {
            sleepTime = 3000;
        }
        this.timeStamp = abs(sleepTime);
    }


    @Override
    public void run(){
        while (!Thread.currentThread().isInterrupted()) {
            try {
                library.startReading();
                Thread.sleep(timeStamp);
                library.stopReading();
            } catch (InterruptedException _) {
                logger.error("ERROR");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
