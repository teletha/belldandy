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

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

class Task<V> extends FutureTask<V> implements ScheduledFuture<V> {

    /** The next trigger time. */
    volatile Instant time;

    /** The interval calculator. */
    final UnaryOperator<Instant> interval;

    Task(Callable<V> task, Instant next, UnaryOperator<Instant> interval) {
        super(task);

        this.time = next;
        this.interval = interval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        if (interval == null) {
            // one shot
            super.run();
        } else {
            // periodically
            runAndReset();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(time.toEpochMilli() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Delayed other) {
        if (other instanceof Task task) {
            return time.compareTo(task.time);
        } else {
            return 0;
        }
    }
}