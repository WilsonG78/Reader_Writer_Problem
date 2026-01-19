***Reader Writer Problem***
***

This project is implementation of Readers Writer Problem.
Our assumptions are 
* there can be only one writer in library,
* there can be at most 5 readers in library,
* the queue is justified.

***
**Implementation**

We use 3 classes Library Writer and Reader class. Library is controller of threads where 
Reader and Writer class is implementation of Runnable interface.


***
**How to build and use**

mvn clean package 

Then start with command

java -jar target/Reader-Writer-Problem-1.0-SNAPSHOT.jar