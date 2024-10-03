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

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class Task<V> extends FutureTask<V> implements ScheduledFuture<V> {

    /** The next trigger time. */
    final AtomicLong time;

    /** The interval time. */
    final long period;

    Task(Callable<V> task, long next, long period) {
        super(task);

        this.time = new AtomicLong(next);
        this.period = period;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        if (period == 0) {
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
        return unit.convert(time.get() - System.nanoTime(), TimeUnit.NANOSECONDS);
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
            long diff = time.get() - task.time.get();
            if (diff < 0) {
                return -1;
            } else if (diff > 0) {
                return 1;
            } else {
                return 1;
            }
        }
        long d = (getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS));
        return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
    }
}