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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

class Task<V> extends FutureTask<V> implements ScheduledFuture<V> {

    /** The next trigger time. */
    final AtomicReference<Instant> time;

    /** The interval calculator. */
    final UnaryOperator<Instant> interval;

    Task(Callable<V> task, Instant next, UnaryOperator<Instant> interval) {
        super(task);

        this.time = new AtomicReference(next);
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
        return unit.convert(time.get().toEpochMilli() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Delayed other) {
        if (other == this) {
            return 0;
        }
        if (other instanceof Task task) {
            return time.get().compareTo((Instant) task.time.get());
        }

        long d = (getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS));
        return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
    }
}