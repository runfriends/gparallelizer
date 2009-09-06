package org.gparallelizer.scheduler;

import org.gparallelizer.actors.pooledActors.Pool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prototype of self-regulated thread pooled scheduler
 *
 * Self regulation happened according to following rules
 * - worker thread, which had nothing to do 10 seconds dies
 * - if no tasks were taken for processing during last 0.5sec new worker starts
 */
public final class Scheduler implements Pool {
    protected final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

    protected AtomicInteger threadCount  = new AtomicInteger();

    protected volatile long lastTaskPoke = -10;

    protected volatile long schedulerTime;

    protected volatile boolean terminating;

    private final int coreSize;

    public Scheduler () {
        this(0);
    }

    public Scheduler (int coreSize) {
        this.coreSize = coreSize;
        new WatchdogThread().start();

        for (int i = 0; i != coreSize; ++i) {
            startNewThread();
        }
    }

    public void execute(Runnable task) {
        if (terminating)
            throw new RuntimeException("Scheduler is shutting down");

        try {
            queue.put(task);
            if (threadCount.get() == 0) {
                startNewThread();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void startNewThread() {
            threadCount.incrementAndGet();
            new WorkerThread().start();
    }

    public void resize(int poolSize) {
        throw new UnsupportedOperationException();
    }

    public void resetDefaultSize() {
        throw new UnsupportedOperationException();
    }

    public void shutdown () {
        terminating = true;
        final int count = threadCount.get();
        for (int i = 0; i != count; ++i)
            try {
                queue.put(new Runnable(){
                    public void run() {
                        throw new RuntimeException("terminate");
                    }
                });
            } catch (InterruptedException e) { //
                Thread.currentThread().interrupt();
            }
    }

    private class WorkerThread extends Thread {
        {
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                try {
                    while (!terminating) {
                        final Runnable task = queue.poll(10, TimeUnit.SECONDS);
                        if (task == null) {
                            return;
                        }

                        lastTaskPoke = schedulerTime;
                        try {
                            task.run();
                        }
                        catch (Throwable t){
                            // ignore
                        }
                    }
                } catch (InterruptedException e) {
                }
            }
            finally {
                threadCount.decrementAndGet();
            }
        }
    }

    private class WatchdogThread extends Thread {
        {
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!terminating) {
                try {
                    schedulerTime++;
                    if (schedulerTime > lastTaskPoke + 10)
                        startNewThread();
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
