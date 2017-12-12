package org.sifrproject.server;

public interface StartStopJoinRunnable extends Runnable {
    void shutdown();
    void start();
    void join();
}
