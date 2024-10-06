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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

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

    /** The task queue. */
    protected final DelayQueue<Task> queue = new DelayQueue();

    public Scheduler() {
        Thread.ofVirtual().start(() -> {
            try {
                while (running.get()) {
                    queue.take().thread.start();
                }
            } catch (InterruptedException e) {
                // stop
            }
        });
    }

    /**
     * Execute the task.
     * 
     * @param task
     */
    protected void executeTask(Task<?> task) {
        if (!task.isCancelled()) {
            runningTask.incrementAndGet();

            // Threads are created when a task is registered, but execution is delayed until the
            // scheduled time. Although it would be simpler to immediately schedule the task using
            // Thread#sleep after execution, this implementation method is used to reduce memory
            // usage as much as possible. Note that only the creation of the thread is done first,
            // since the information is not inherited by InheritableThreadLocal if the thread is
            // simply placed in the task queue.
            task.thread = Thread.ofVirtual().unstarted(() -> {
                try {
                    if (!task.isCancelled()) {
                        task.run();
                        executedTask.incrementAndGet();

                        if (task.interval == null) {
                            // one shot
                        } else {
                            // reschedule task
                            task.next = task.interval.applyAsLong(task.next);
                            executeTask(task);
                        }
                    }
                } finally {
                    runningTask.decrementAndGet();
                }
            });
            queue.add(task);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return schedule(Executors.callable(command), delay, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> command, long delay, TimeUnit unit) {
        Task<V> task = new Task(command, nextMilli(delay, unit), null);
        executeTask(task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long delay, long interval, TimeUnit unit) {
        Task task = new Task<>(callable(command), nextMilli(delay, unit), old -> old + unit.toMillis(interval));
        executeTask(task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long delay, long interval, TimeUnit unit) {
        Task task = new Task<>(callable(command), nextMilli(delay, unit), old -> System.currentTimeMillis() + unit.toMillis(interval));
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
        Field[] fields = parse(format);
        LongUnaryOperator next = old -> next(fields, ZonedDateTime.now()).toInstant().toEpochMilli();

        Task task = new Task(callable(command), next.applyAsLong(0L), old -> next.applyAsLong(0L));
        executeTask(task);
        return task;
    }

    /**
     * Parses a cron expression into an array of {@link Field} objects.
     * The cron expression is expected to have 5 or 6 parts:
     * - For a standard cron expression with 5 parts (minute, hour, day of month, month, day of
     * week), the seconds field will be assumed to be "0".
     * - For a cron expression with 6 parts (second, minute, hour, day of month, month, day of
     * week), all fields are used directly from the cron expression.
     *
     * @param cron the cron expression to parse
     * @return an array of {@link Field} objects representing the parsed cron fields.
     * @throws IllegalArgumentException if the cron expression does not have 5 or 6 parts
     */
    static Field[] parse(String cron) {
        String[] parts = cron.split("\\s+");
        int i = switch (parts.length) {
        case 5 -> 0;
        case 6 -> 1;
        default -> throw new IllegalArgumentException(cron);
        };

        return new Field[] { //
                new Field(Type.SECOND, i == 1 ? parts[0] : "0"), new Field(Type.MINUTE, parts[i++]), new Field(Type.HOUR, parts[i++]),
                new Field(Type.DAY_OF_MONTH, parts[i++]), new Field(Type.MONTH, parts[i++]), new Field(Type.DAY_OF_WEEK, parts[i++])};
    }

    /**
     * Calculates the next execution time based on the provided cron fields and a base time.
     * 
     * The search for the next execution time will start from the base time and continue until
     * a matching time is found. The search will stop if no matching time is found within four
     * years.
     * 
     * @param cron an array of {@link Field} objects representing the parsed cron fields
     * @param base the {@link ZonedDateTime} representing the base time to start the search from
     * @return the next execution time as a {@link ZonedDateTime}
     * @throws IllegalArgumentException if no matching execution time is found within four years
     */
    static ZonedDateTime next(Field[] cron, ZonedDateTime base) {
        // The range is four years, taking into account leap years.
        ZonedDateTime limit = base.plusYears(4);

        ZonedDateTime[] next = {base.plusSeconds(1).truncatedTo(ChronoUnit.SECONDS)};
        root: while (true) {
            if (next[0].isAfter(limit)) throw new IllegalArgumentException("Next time is not found before " + limit);
            if (!cron[4].nextMatch(next)) continue;

            int month = next[0].getMonthValue();
            while (!(cron[3].matchesDay(next[0].toLocalDate()) && cron[5].matchesDoW(next[0].toLocalDate()))) {
                next[0] = next[0].plusDays(1).truncatedTo(ChronoUnit.DAYS);
                if (next[0].getMonthValue() != month) continue root;
            }

            if (!cron[2].nextMatch(next)) continue;
            if (!cron[1].nextMatch(next)) continue;
            if (!cron[0].nextMatch(next)) continue;
            return next[0];
        }
    }

    static long nextMilli(long delay, TimeUnit unit) {
        return System.currentTimeMillis() + unit.toMillis(delay);
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
        return new Task(callable, 0, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return newTaskFor(Executors.callable(runnable, value));
    }
}