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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.ToLongFunction;

import kiss.I;

public class TestableScheduler extends Scheduler {

    private Map<Object, Future> futures = new ConcurrentHashMap();

    private long awaitingLimit = 1000;

    private final AtomicBoolean starting = new AtomicBoolean();

    private List<Task> startingBuffer = new ArrayList();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeTask(Task task) {
        if (starting.get()) {
            super.executeTask(task);
        } else {
            startingBuffer.add(task);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Task task = new Task(callable(command), calculateNext(delay, unit), null);
        executeTask(task);

        futures.put(command, task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> command, long delay, TimeUnit unit) {
        Task task = new Task(command, calculateNext(delay, unit), null);
        executeTask(task);

        futures.put(command, task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        Task task = new Task<>(callable(command), calculateNext(initialDelay, unit), old -> old + unit.toMillis(period));
        executeTask(task);

        futures.put(command, task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        Task task = new Task<>(callable(command), calculateNext(initialDelay, unit), old -> System.currentTimeMillis() + unit
                .toMillis(delay));
        executeTask(task);

        futures.put(command, task);
        return task;
    }

    @Override
    public ScheduledFuture<?> scheduleAt(Runnable command, String fromat) {
        Cron cron = new Cron(fromat);
        ToLongFunction<Long> next = prev -> {
            return cron.next(ZonedDateTime.now()).toEpochSecond() * 1000;
        };

        Task task = new Task(callable(command), next.applyAsLong(0L), old -> next.applyAsLong(0L));
        executeTask(task);

        futures.put(command, task);
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

        while (count++ == 0 || runningTask.getAcquire() != 0) {
            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                throw I.quiet(e);
            }

            if (awaitingLimit <= System.currentTimeMillis() - start) {
                throw new Error("Too long task is active. RunningTask:" + runningTask.get() + "  ExecutedTask:" + executedTask);
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
