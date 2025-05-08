package ru.mephi.customthreadpoolservice.custom;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class CustomThreadFactory implements ThreadFactory {

    private static final Logger LOGGER = Logger.getLogger(CustomThreadFactory.class.getName());
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, "MyPool-worker-" + threadNumber.getAndIncrement());
        LOGGER.info("Creating new thread: " + thread.getName() + ".");
        return thread;
    }
}
