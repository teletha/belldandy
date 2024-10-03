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

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class Scheduler extends AbstractExecutorService implements ScheduledExecutorService {

    /** The running state of task queue. */
    private final AtomicBoolean running = new AtomicBoolean(true);

    /** The counter for the running tasks. */
    protected final AtomicLong runningTask = new AtomicLong();

    /** The counter for the executed tasks. */
    protected final AtomicLong executedTask = new AtomicLong();

    /** The thread factory. */
    protected Function<Runnable, Thread> factory = Thread::startVirtualThread;

    /**
     * Execute the task.
     * 
     * @param task
     */
    protected void executeTask(Task task) {
        if (!task.isCancelled()) {
            runningTask.incrementAndGet();

            factory.apply(() -> {
                try {
                    Thread.sleep(Duration.ofNanos(task.getDelay(TimeUnit.NANOSECONDS)));

                    if (!task.isCancelled()) {
                        task.run();

                        if (task.period == 0) {
                            // one shot
                        } else {
                            // reschedule task
                            if (task.period > 0) {
                                // fixed rate
                                task.time.addAndGet(task.period);
                            } else {
                                // fixed delay
                                task.time.set(calculateNext(-task.period, TimeUnit.NANOSECONDS));
                            }
                            executeTask(task);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    runningTask.decrementAndGet();
                    executedTask.incrementAndGet();
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Task task = new Task(callable(command), calculateNext(delay, unit), 0);
        executeTask(task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> command, long delay, TimeUnit unit) {
        Task<V> task = new Task(command, calculateNext(delay, unit), 0);
        executeTask(task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        Task task = new Task(callable(command), calculateNext(initialDelay, unit), unit.toNanos(period));
        executeTask(task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        Task task = new Task(callable(command), calculateNext(initialDelay, unit), unit.toNanos(-delay));
        executeTask(task);
        return task;
    }

    public ScheduledFuture<?> scheduleAt(Runnable command, String cron) {
        Cron c = new Cron(cron);
        ZonedDateTime now = ZonedDateTime.now();
        Instant next = c.nextTimeAfter(now).toInstant();
        long nano = next.getEpochSecond() * 1000000000 + next.getNano();

        Task task = new Task(callable(command), nano, 0);
        executeTask(task);
        return task;
    }

    long calculateNext(long delay, TimeUnit unit) {
        return System.nanoTime() + unit.toNanos(delay);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        running.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Runnable> shutdownNow() {
        running.set(false);
        List<Runnable> remainingTasks = new ArrayList<>();
        return remainingTasks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isShutdown() {
        return !running.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTerminated() {
        return isShutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long remainingNanos = unit.toNanos(timeout);
        long end = System.nanoTime() + remainingNanos;
        while (remainingNanos > 0) {
            if (isTerminated()) {
                return true;
            }
            Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(remainingNanos) + 1, 100));
            remainingNanos = end - System.nanoTime();
        }
        return isTerminated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Runnable command) {
        schedule(command, 0, TimeUnit.NANOSECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new Task(callable, 0, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return newTaskFor(Executors.callable(runnable, value));
    }

    protected static ZonedDateTime next(ZonedDateTime base, int month, int day, int hour, int minute) {
        base = find(base, minute, ChronoField.MINUTE_OF_HOUR, ChronoUnit.HOURS);
        base = find(base, hour, ChronoField.HOUR_OF_DAY, ChronoUnit.DAYS);
        base = find(base, day, ChronoField.DAY_OF_MONTH, ChronoUnit.MONTHS);
        base = find(base, month, ChronoField.MONTH_OF_YEAR, ChronoUnit.YEARS);

        return base;
    }

    private static ZonedDateTime find(ZonedDateTime base, int value, ChronoField target, ChronoUnit upper) {
        try {
            if (value < 0) {
                return base;
            } else if (base.get(target) <= value) {
                return base.with(target, value);
            } else {
                return base.plus(1, upper).with(target, value);
            }
        } catch (DateTimeException e) {
            return find(base.plus(1, upper), value, target, upper);
        }
    }
}