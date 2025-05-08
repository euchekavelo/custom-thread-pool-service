package ru.mephi.customthreadpoolservice.custom;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

public class CustomRejectionHandler implements RejectedExecutionHandler {

    private static final Logger LOGGER = Logger.getLogger(CustomRejectionHandler.class.getName());

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        LOGGER.warning("Task " + r.toString() + " was rejected due to overload!");
        throw new RejectedExecutionException("Task rejected.");
    }
}
