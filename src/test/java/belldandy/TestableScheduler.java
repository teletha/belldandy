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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import kiss.I;

public class TestableScheduler extends Scheduler {

    private long awaitingLimit = 1000;

    private final AtomicBoolean starting = new AtomicBoolean();

    private List<Task> startingBuffer = new ArrayList();

    private AtomicLong executed = new AtomicLong();

    /**
     * 
     */
    TestableScheduler() {
        super();
    }

    /**
     * @param limit
     */
    TestableScheduler(int limit) {
        super(limit);
    }

    private Runnable wrap(Runnable task) {
        return () -> {
            try {
                task.run();
            } finally {
                executed.incrementAndGet();
            }
        };
    }

    private <V> Callable<V> wrap(Callable<V> task) {
        return () -> {
            try {
                return task.call();
            } finally {
                executed.incrementAndGet();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return super.schedule(wrap(command), delay, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> command, long delay, TimeUnit unit) {
        return super.schedule(wrap(command), delay, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long delay, long interval, TimeUnit unit) {
        return super.scheduleAtFixedRate(wrap(command), delay, interval, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long delay, long interval, TimeUnit unit) {
        return super.scheduleWithFixedDelay(wrap(command), delay, interval, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleAt(Runnable command, String format) {
        return super.scheduleAt(wrap(command), format);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Task executeTask(Task task) {
        if (starting.get()) {
            super.executeTask(task);
        } else {
            startingBuffer.add(task);
        }
        return task;
    }

    /**
     * Start task handler thread.
     * 
     * @return
     */
    protected final TestableScheduler start() {
        if (starting.compareAndSet(false, true)) {
            for (Task task : startingBuffer) {
                super.executeTask(task);
            }
            startingBuffer.clear();
        }
        return this;
    }

    /**
     * Await any task is running.
     */
    protected boolean awaitRunning() {
        int count = 0; // await at least once
        long start = System.currentTimeMillis();
        while (count++ == 0 || runs.isEmpty()) {
            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                throw I.quiet(e);
            }

            if (awaitingLimit <= System.currentTimeMillis() - start) {
                throw new Error("No task is active. " + this);
            }
        }
        return true;
    }

    protected TestableScheduler limitAwaitTime(long millis) {
        awaitingLimit = millis;
        return this;
    }

    /**
     * Await all tasks are executed.
     */
    protected boolean awaitIdling() {
        int count = 0; // await at least once
        long start = System.currentTimeMillis();

        while (count++ == 0 || !queue.isEmpty() || !runs.isEmpty()) {
            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                throw I.quiet(e);
            }

            if (awaitingLimit <= System.currentTimeMillis() - start) {
                throw new Error("Too long task is active. " + this);
            }
        }
        return true;
    }

    /**
     * Awaits until the specified number of tasks have been executed.
     * 
     * @param required
     * @return
     */
    protected boolean awaitExecutions(long required) {
        long start = System.currentTimeMillis();

        while (executed.get() < required) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw I.quiet(e);
            }

            if (awaitingLimit <= System.currentTimeMillis() - start) {
                throw new Error("Too long task is active. " + this);
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Executor [running: " + runs.size() + " executed: " + executed + " queue: " + queue + "]";
    }
}
