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
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

/**
 * A custom scheduler implementation based on the {@link ScheduledExecutorService} interface,
 * using virtual threads to schedule tasks with specified delays or intervals.
 * 
 * <p>
 * This class extends {@link AbstractExecutorService} and implements
 * {@link ScheduledExecutorService} to provide scheduling capabilities with a task queue and delay
 * mechanisms. It leverages {@link DelayQueue} to manage task execution times, and uses virtual
 * threads to run tasks in a lightweight and efficient manner.
 * </p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 * <li>Allows scheduling of tasks with delays or fixed intervals.</li>
 * <li>Supports scheduling based on cron expressions for periodic execution.</li>
 * <li>Uses virtual threads to minimize memory consumption and resource overhead.</li>
 * </ul>
 * 
 * <h2>Thread Management</h2>
 * <p>
 * Virtual threads are created in an "unstarted" state when tasks are registered. Execution is
 * delayed until the scheduled time, reducing memory usage. Once the scheduled time arrives, the
 * virtual threads are started and the tasks are executed. If the task is periodic, it is
 * rescheduled after completion.
 * </p>
 * 
 * <h2>Usage</h2>
 * <p>
 * You can use the following methods to schedule tasks:
 * <ul>
 * <li>{@link #schedule(Runnable, long, TimeUnit)}: Schedule a task with a delay.</li>
 * <li>{@link #scheduleAtFixedRate(Runnable, long, long, TimeUnit)}: Schedule a task at fixed
 * intervals.</li>
 * <li>{@link #scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}: Schedule a task with a fixed
 * delay between executions.</li>
 * <li>{@link #scheduleAt(Runnable, String)}: Schedule a task based on a cron expression.</li>
 * </ul>
 * </p>
 * 
 * <h2>Task Lifecycle</h2>
 * <p>
 * The scheduler maintains internal counters to track running tasks and completed tasks using
 * {@link AtomicLong}. The task queue is managed through {@link DelayQueue}, which ensures tasks
 * are executed at the correct time. Each task is wrapped in a custom {@link Task} class that
 * handles execution, cancellation, and rescheduling (for periodic tasks).
 * </p>
 * 
 * <h2>Shutdown and Termination</h2>
 * <p>
 * The scheduler can be shut down using the {@link #shutdown()} or {@link #shutdownNow()} methods,
 * which stops the execution of any further tasks. The {@link #awaitTermination(long, TimeUnit)}
 * method can be used to block until all tasks are finished executing after a shutdown request.
 * </p>
 * 
 * @see ScheduledExecutorService
 */
public class Scheduler extends AbstractExecutorService implements ScheduledExecutorService {

    /** The the running task manager. */
    protected final Set<Task> runs = ConcurrentHashMap.newKeySet();

    /** The task queue. */
    protected DelayQueue<Task> queue = new DelayQueue();

    /** The running state of task queue. */
    private volatile boolean run = true;

    /** Controls the number of tasks that can be executed concurrently. */
    private final Semaphore max;

    public Scheduler() {
        this(Integer.MAX_VALUE);
    }

    /**
     * @param limit Controls the number of tasks that can be executed concurrently.
     */
    public Scheduler(int limit) {
        max = new Semaphore(limit);

        Thread.ofVirtual().start(() -> {
            try {
                while (run || !queue.isEmpty()) {
                    max.acquire();
                    Task task = queue.take();
                    // Task execution state management is performed before thread execution because
                    // it is too slow if the task execution state management is performed within the
                    // task's execution thread.
                    runs.add(task);

                    // execute task actually
                    task.thread.start();
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
    protected Task executeTask(Task<?> task) {
        if (!run) {
            throw new RejectedExecutionException();
        }

        if (!task.isCancelled()) {
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

                        if (task.interval == null || !run) {
                            // one shot or scheduler is already stopped
                        } else {
                            // reschedule task
                            task.next = task.interval.applyAsLong(task.next);
                            executeTask(task);
                        }
                    }
                } finally {
                    runs.remove(task);
                    max.release();
                }
            });
            queue.add(task);
        }

        return task;
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
        return executeTask(new Task(command, next(delay, unit), null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long delay, long interval, TimeUnit unit) {
        return executeTask(new Task(callable(command), next(delay, unit), old -> old + unit.toMillis(interval)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long delay, long interval, TimeUnit unit) {
        return executeTask(new Task(callable(command), next(delay, unit), old -> System.currentTimeMillis() + unit.toMillis(interval)));
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
        Cron[] fields = parse(format);
        LongUnaryOperator next = old -> next(fields, ZonedDateTime.now()).toInstant().toEpochMilli();

        return executeTask(new Task(callable(command), next.applyAsLong(0L), next));
    }

    /**
     * Parses a cron expression into an array of {@link Cron} objects.
     * The cron expression is expected to have 5 or 6 parts:
     * - For a standard cron expression with 5 parts (minute, hour, day of month, month, day of
     * week), the seconds field will be assumed to be "0".
     * - For a cron expression with 6 parts (second, minute, hour, day of month, month, day of
     * week), all fields are used directly from the cron expression.
     *
     * @param cron the cron expression to parse
     * @return an array of {@link Cron} objects representing the parsed cron fields.
     * @throws IllegalArgumentException if the cron expression does not have 5 or 6 parts
     */
    static Cron[] parse(String cron) {
        String[] parts = cron.strip().split("\\s+");
        int i = parts.length - 5;
        if (i != 0 && i != 1) {
            throw new IllegalArgumentException("Invalid cron '" + cron + "'");
        }

        return new Cron[] {new Cron(ChronoField.SECOND_OF_MINUTE, 0, 59, "", "", "/", i == 1 ? parts[0] : "0"),
                new Cron(ChronoField.MINUTE_OF_HOUR, 0, 59, "", "", "/", parts[i++]),
                new Cron(ChronoField.HOUR_OF_DAY, 0, 23, "", "", "/", parts[i++]),
                new Cron(ChronoField.DAY_OF_MONTH, 1, 31, "", "?LW", "/", parts[i++]),
                new Cron(ChronoField.MONTH_OF_YEAR, 1, 12, "JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC ", "", "/", parts[i++]),
                new Cron(ChronoField.DAY_OF_WEEK, 1, 7, "MON TUE WED THU FRI SAT SUN ", "?L", "#/", parts[i++])};
    }

    /**
     * Calculates the next execution time based on the provided cron fields and a base time.
     * 
     * The search for the next execution time will start from the base time and continue until
     * a matching time is found. The search will stop if no matching time is found within four
     * years.
     * 
     * @param cron an array of {@link Cron} objects representing the parsed cron fields
     * @param base the {@link ZonedDateTime} representing the base time to start the search from
     * @return the next execution time as a {@link ZonedDateTime}
     * @throws IllegalArgumentException if no matching execution time is found within four years
     */
    static ZonedDateTime next(Cron[] cron, ZonedDateTime base) {
        // The range is four years, taking into account leap years.
        ZonedDateTime limit = base.plusYears(4);

        ZonedDateTime[] next = {base.plusSeconds(1).truncatedTo(ChronoUnit.SECONDS)};
        root: while (true) {
            if (next[0].isAfter(limit)) throw new IllegalArgumentException("Next time is not found before " + limit);
            if (!cron[4].nextMatch(next)) continue;

            int month = next[0].getMonthValue();
            while (!(cron[3].matches(next[0]) && cron[5].matches(next[0]))) {
                next[0] = next[0].plusDays(1).truncatedTo(ChronoUnit.DAYS);
                if (next[0].getMonthValue() != month) continue root;
            }

            if (!cron[2].nextMatch(next)) continue;
            if (!cron[1].nextMatch(next)) continue;
            if (!cron[0].nextMatch(next)) continue;
            return next[0];
        }
    }

    /**
     * Calculates the next time point by adding the specified delay to the current system time.
     * 
     * This method takes the current system time (in milliseconds) and adds the provided delay,
     * which is converted to milliseconds based on the provided {@link TimeUnit}. The result is the
     * time point (in milliseconds since the Unix epoch) that corresponds to the current time plus
     * the delay.
     * 
     * @param delay the delay to add to the current time
     * @param unit the {@link TimeUnit} representing the unit of the delay (e.g., seconds, minutes)
     * @return the next time point in milliseconds since the Unix epoch
     */
    static long next(long delay, TimeUnit unit) {
        return System.currentTimeMillis() + unit.toMillis(delay);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        run = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Runnable> shutdownNow() {
        run = false;
        for (Task run : runs) {
            run.thread.interrupt();
        }

        DelayQueue temp = queue;
        queue = new DelayQueue();
        return new ArrayList(temp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean awaitTermination(long time, TimeUnit unit) throws InterruptedException {
        long end = next(time, unit);
        while (!isTerminated()) {
            long rem = end - System.currentTimeMillis();
            if (rem < 0) {
                return false;
            }
            Thread.sleep(Math.min(rem + 1, 100));
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isShutdown() {
        return !run;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTerminated() {
        return !run && queue.isEmpty() && runs.isEmpty();
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