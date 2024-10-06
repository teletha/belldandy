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
import java.util.concurrent.atomic.AtomicBoolean;

import kiss.I;

public class TestableScheduler extends Scheduler {

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
        while (count++ == 0 || runningTask.getAcquire() == 0) {
            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                throw I.quiet(e);
            }

            if (awaitingLimit <= System.currentTimeMillis() - start) {
                throw new Error("No task is active. RunningTask:" + runningTask.get() + "  ExecutedTask:" + executedTask);
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

        while (count++ == 0 || !queue.isEmpty() || runningTask.getAcquire() != 0) {
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
     * Awaits until the specified number of tasks have been executed.
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Executor [running: " + runningTask + " executed: " + executedTask + "]";
    }
}
