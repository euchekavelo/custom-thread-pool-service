package ru.mephi.customthreadpoolservice.custom;

import java.util.logging.Logger;

public class SimulationTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(SimulationTask.class.getName());

    @Override
    public void run() {
        try {
            LOGGER.info("Task simulation started.");
            Thread.sleep(25000);
            LOGGER.info("Task simulation finished.");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
