/*
 * Copyright (C) 2023 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package belldandy;

import static java.util.concurrent.Executors.*;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import kiss.I;

public class TestableExecutor extends VirtualScheduler {

    private Map<Object, Future> futures = new ConcurrentHashMap();

    private long awaitingLimit = 1000;

    public TestableExecutor() {
        super(Thread.ofVirtual()::unstarted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        ScheduledFutureTask task = new ScheduledFutureTask(callable(command), calculateNext(delay, unit), 0);
        taskQueue.offer(task);

        futures.put(command, task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> command, long delay, TimeUnit unit) {
        ScheduledFutureTask task = new ScheduledFutureTask(command, calculateNext(delay, unit), 0);
        taskQueue.offer(task);

        futures.put(command, task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        ScheduledFutureTask task = new ScheduledFutureTask(callable(command), calculateNext(initialDelay, unit), unit.toNanos(period));
        taskQueue.offer(task);

        futures.put(command, task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        ScheduledFutureTask task = new ScheduledFutureTask(callable(command), calculateNext(initialDelay, unit), unit.toNanos(-delay));
        taskQueue.offer(task);

        futures.put(command, task);
        return task;
    }

    /**
     * Start task handler thread.
     * 
     * @return
     */
    protected final TestableExecutor start() {
        monitor.start();
        return this;
    }

    protected TestableExecutor limitAwaitTime(long millis) {
        awaitingLimit = millis;
        return this;
    }

    /**
     * Await all tasks are executed.
     */
    protected boolean awaitIdling() {
        int count = 0; // await at least once
        long start = System.currentTimeMillis();

        while (count++ == 0 || !taskQueue.isEmpty() || runningTask.getAcquire() != 0) {
            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                throw I.quiet(e);
            }

            if (awaitingLimit <= System.currentTimeMillis() - start) {
                throw new Error("Too long task is active. TaskQueue:" + taskQueue.size() + " RunningTask:" + runningTask
                        .get() + "  ExecutedTask:" + executedTask);
            }
        }
        return true;
    }

    /**
     * Await the required tasks are executed.
     * 
     * @param required
     * @return
     */
    protected boolean awaitExecutions(long required) {
        long start = System.currentTimeMillis();

        while (executedTask.get() < required) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw I.quiet(e);
            }

            if (awaitingLimit <= System.currentTimeMillis() - start) {
                throw new Error("Too long task is active.");
            }
        }
        return true;
    }

    protected void cancel(Object command) {
        Future future = futures.get(command);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Executor [running: " + runningTask + " executed: " + executedTask + "]";
    }
}
