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

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
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
import java.util.function.UnaryOperator;

/**
 * A custom implementation of {@link ScheduledExecutorService} that provides
 * advanced scheduling capabilities including cron-based scheduling.
 * <p>
 * This class extends {@link AbstractExecutorService} and implements
 * {@link ScheduledExecutorService}, offering methods to schedule tasks
 * for one-time execution, fixed-rate execution, fixed-delay execution,
 * and cron-based execution.
 * </p>
 * <p>
 * Key features:
 * <ul>
 * <li>Uses virtual threads for task execution, improving scalability.</li>
 * <li>Supports standard scheduling methods like {@code schedule},
 * {@code scheduleAtFixedRate}, and {@code scheduleWithFixedDelay}.</li>
 * <li>Provides a custom {@code scheduleAt} method for cron-based scheduling.</li>
 * <li>Allows customization of the thread factory.</li>
 * </ul>
 * </p>
 * <p>
 * This scheduler is designed to be flexible and efficient, suitable for
 * applications requiring complex scheduling patterns or high concurrency.
 * </p>
 * 
 * @see java.util.concurrent.ScheduledExecutorService
 */
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
    protected void executeTask(Task<?> task) {
        if (!task.isCancelled()) {
            runningTask.incrementAndGet();

            factory.apply(() -> {
                try {
                    Thread.sleep(Duration.between(Instant.now(), task.time.get()));

                    if (!task.isCancelled()) {
                        task.run();

                        if (task.interval == null) {
                            // one shot
                        } else {
                            // reschedule task
                            Instant next = task.interval.apply(task.time.get());
                            task.time.set(next);
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
        Task task = new Task(callable(command), calculateNext(delay, unit), null);
        executeTask(task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> command, long delay, TimeUnit unit) {
        Task<V> task = new Task(command, calculateNext(delay, unit), null);
        executeTask(task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long delay, long interval, TimeUnit unit) {
        Task task = new Task<>(callable(command), calculateNext(delay, unit), old -> old.plus(interval, unit.toChronoUnit()));
        executeTask(task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long delay, long interval, TimeUnit unit) {
        Task task = new Task<>(callable(command), calculateNext(delay, unit), old -> Instant.now().plus(interval, unit.toChronoUnit()));
        executeTask(task);
        return task;
    }

    /**
     * Schedules a task to be executed periodically based on a cron expression.
     * <p>
     * This method uses a cron expression to determine the execution intervals for the given
     * {@code Runnable} command.
     * It creates a task that calculates the next execution time using the provided cron format. The
     * task is executed at each calculated interval, and the next execution time is determined
     * dynamically after each run.
     * </p>
     * 
     * @param command The {@code Runnable} task to be scheduled for periodic execution.
     * @param format A valid cron expression that defines the schedule for task execution.
     *            The cron format is parsed to calculate the next execution time.
     * 
     * @return A {@code ScheduledFuture<?>} representing the pending completion of the task.
     *         The {@code ScheduledFuture} can be used to cancel or check the status of the task.
     * 
     * @throws IllegalArgumentException If the cron format is invalid or cannot be parsed correctly.
     */
    public ScheduledFuture<?> scheduleAt(Runnable command, String format) {
        Cron cron = new Cron(format);
        UnaryOperator<Instant> next = prev -> {
            return cron.next(ZonedDateTime.now()).toInstant();
        };

        Task task = new Task(callable(command), next.apply(Instant.EPOCH), old -> next.apply(Instant.EPOCH));
        executeTask(task);
        return task;
    }

    Instant calculateNext(long delay, TimeUnit unit) {
        return Instant.now().plus(delay, unit.toChronoUnit());
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
        long remaining = unit.toMillis(timeout);
        long end = System.currentTimeMillis() + remaining;
        while (remaining > 0) {
            if (isTerminated()) {
                return true;
            }
            Thread.sleep(Math.min(remaining + 1, 100));
            remaining = end - System.currentTimeMillis();
        }
        return isTerminated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Runnable command) {
        schedule(command, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new Task(callable, Instant.EPOCH, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return newTaskFor(Executors.callable(runnable, value));
    }
}