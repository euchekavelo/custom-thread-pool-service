package ru.mephi.customthreadpoolservice.custom;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class CustomThreadPool implements CustomExecutor {

    private static final Logger LOGGER = Logger.getLogger(CustomThreadPool.class.getName());
    private final int corePoolSize;
    private final int maxPoolSize;
    private final int minSpareThreads;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;

    private final List<WorkerThread> workers;
    private final List<BlockingQueue<Runnable>> queues;
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger currentQueueIndex = new AtomicInteger(0);
    private final AtomicInteger idleThreads = new AtomicInteger(0);
    private volatile boolean isShutdown = false;

    private final CustomThreadFactory threadFactory;
    private final RejectedExecutionHandler rejectionHandler;

    public CustomThreadPool(int corePoolSize, int maxPoolSize, int minSpareThreads, long keepAliveTime,
                            TimeUnit timeUnit, int queueSize) {

        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.minSpareThreads = minSpareThreads;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueSize = queueSize;

        this.workers = new CopyOnWriteArrayList<>();
        this.queues = new CopyOnWriteArrayList<>();
        this.threadFactory = new CustomThreadFactory();
        this.rejectionHandler = new CustomRejectionHandler();

        for (int i = 0; i < corePoolSize; i++) {
            queues.add(new LinkedBlockingQueue<>(queueSize));
        }

        for (int i = 0; i < corePoolSize; i++) {
            createWorker(i);
        }
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            throw new RejectedExecutionException("ThreadPool is shutdown.");
        }

        if (idleThreads.get() < minSpareThreads && activeThreads.get() < maxPoolSize) {
            queues.add(new LinkedBlockingQueue<>(queueSize));
            int newQueueIndex = queues.size() - 1;
            createWorker(newQueueIndex);
        }

        // Round Robin распределение
        int index = currentQueueIndex.getAndIncrement() % queues.size();
        BlockingQueue<Runnable> queue = queues.get(index);

        try {
            if (!queue.offer(command)) {
                if (activeThreads.get() < maxPoolSize) {
                    queues.add(new LinkedBlockingQueue<>(queueSize));
                    int newQueueIndex = queues.size() - 1;
                    createWorker(newQueueIndex);
                    queue = queues.get(newQueueIndex);
                    if (!queue.offer(command)) {
                        rejectionHandler.rejectedExecution(command, null);
                    }
                } else {
                    rejectionHandler.rejectedExecution(command, null);
                }
            }
            LOGGER.info("Task accepted into queue #" + index + ".");
        } catch (Exception e) {
            LOGGER.severe("Error while executing task: " + e.getMessage());
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> futureTask = new FutureTask<>(callable);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        for (WorkerThread worker : workers) {
            worker.shutdown();
        }
    }

    @Override
    public void shutdownNow() {
        isShutdown = true;
        for (BlockingQueue<Runnable> queue : queues) {
            queue.clear();
        }
        for (WorkerThread worker : workers) {
            worker.shutdownNow();
        }
    }

    private void createWorker(int queueIndex) {
        WorkerThread worker = new WorkerThread(queues.get(queueIndex));
        Thread thread = threadFactory.newThread(worker);
        workers.add(worker);
        thread.start();
        activeThreads.incrementAndGet();
    }

    private class WorkerThread implements Runnable {
        private final BlockingQueue<Runnable> queue;
        private volatile boolean isShutdown = false;

        public WorkerThread(BlockingQueue<Runnable> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                while (!isShutdown) {
                    idleThreads.incrementAndGet();
                    try {
                        Runnable task = queue.poll(keepAliveTime, timeUnit);

                        if (task != null) {
                            idleThreads.decrementAndGet();
                            LOGGER.info(Thread.currentThread().getName() + " executes task.");
                            task.run();
                        } else if (activeThreads.get() > corePoolSize) {
                            LOGGER.info(Thread.currentThread().getName() + " idle timeout, stopping.");
                            activeThreads.decrementAndGet();
                            break;
                        }
                    } finally {
                        idleThreads.decrementAndGet();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                LOGGER.info(Thread.currentThread().getName() + " terminated.");
                workers.remove(this);
            }
        }

        public void shutdown() {
            isShutdown = true;
        }

        public void shutdownNow() {
            isShutdown = true;
            Thread.currentThread().interrupt();
        }
    }
}
