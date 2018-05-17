package org.sifrproject.server;

import org.apache.commons.daemon.Daemon;

public interface StartStopJoinRunnable extends Runnable, Daemon{
    @Override
    void stop();
    @Override
    void start();
    void join();
}
