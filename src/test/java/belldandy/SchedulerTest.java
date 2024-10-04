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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class SchedulerTest extends SchedulerTestSupport {

    @RepeatedTest(5)
    void execute() {
        int[] count = {0};
        scheduler.execute(() -> {
            count[0] = 1;
        });
        assert scheduler.start().awaitIdling();
        assert count[0] == 1 : Arrays.toString(count) + "  " + count[0] + scheduler;
    }

    @RepeatedTest(5)
    void submitCallable() {
        Verifier verifier = new Verifier("OK");
        Future<String> future = scheduler.submit((Callable) verifier);
        assert verifyRunning(future);
        assert scheduler.start().awaitIdling();
        assert verifySuccessed(future, "OK");
        assert verifier.verifyExecutionCount(1);
    }

    @RepeatedTest(5)
    void submitCallableCancel() {
        Verifier verifier = new Verifier("OK");
        Future<String> future = scheduler.submit((Callable) verifier);
        future.cancel(false);
        assert scheduler.start().awaitIdling();
        assert verifyCanceled(future);
        assert verifier.verifyExecutionCount(0);
    }

    @RepeatedTest(5)
    void submitRunnable() {
        int[] count = {0};
        Future<?> future = scheduler.submit((Runnable) () -> count[0]++);
        assert verifyRunning(future);
        assert scheduler.start().awaitIdling();
        assert verifySuccessed(future, null);
        assert count[0] == 1;
    }

    @RepeatedTest(5)
    void submitRunnableCancle() {
        int[] count = {0};
        Future<?> future = scheduler.submit((Runnable) () -> count[0]++);
        future.cancel(false);
        assert scheduler.start().awaitIdling();
        assert verifyCanceled(future);
    }

    @RepeatedTest(5)
    void schedule() {
        Verifier verifier = new Verifier("OK");
        ScheduledFuture<String> future = scheduler.schedule((Callable) verifier, 50, TimeUnit.MILLISECONDS);
        assert verifyRunning(future);
        assert scheduler.start().awaitIdling();
        assert verifySuccessed(future, "OK");
        assert verifier.verifyInitialDelay(50);
        assert verifier.verifyExecutionCount(1);
    }

    @RepeatedTest(5)
    void scheduleMultiSameDelay() {
        Verifier verifier1 = new Verifier("1");
        Verifier verifier2 = new Verifier("2");
        Verifier verifier3 = new Verifier("3");
        ScheduledFuture<String> future1 = scheduler.schedule((Callable) verifier1, 50, TimeUnit.MILLISECONDS);
        ScheduledFuture<String> future2 = scheduler.schedule((Callable) verifier2, 50, TimeUnit.MILLISECONDS);
        ScheduledFuture<String> future3 = scheduler.schedule((Callable) verifier3, 50, TimeUnit.MILLISECONDS);
        assert verifyRunning(future1, future2, future3);
        assert scheduler.start().awaitIdling();
        assert verifySuccessed(future1, "1");
        assert verifySuccessed(future2, "2");
        assert verifySuccessed(future3, "3");
        assert verifier1.verifyInitialDelay(50);
        assert verifier1.verifyExecutionCount(1);
        assert verifier2.verifyInitialDelay(50);
        assert verifier2.verifyExecutionCount(1);
        assert verifier3.verifyInitialDelay(50);
        assert verifier3.verifyExecutionCount(1);
    }

    @RepeatedTest(5)
    void scheduleMultiDifferentDelay() {
        Verifier verifier1 = new Verifier();
        Verifier verifier2 = new Verifier();
        Verifier verifier3 = new Verifier();
        ScheduledFuture<String> future1 = scheduler.schedule((Callable) verifier1, 100, TimeUnit.MILLISECONDS);
        ScheduledFuture<String> future2 = scheduler.schedule((Callable) verifier2, 50, TimeUnit.MILLISECONDS);
        ScheduledFuture<String> future3 = scheduler.schedule((Callable) verifier3, 20, TimeUnit.MILLISECONDS);
        assert verifyRunning(future1, future2, future3);
        assert scheduler.start().awaitIdling();
        assert verifySuccessed(future1, future2, future3);
        assert verifyExecutionOrder(verifier3, verifier2, verifier1);
    }

    @RepeatedTest(5)
    void scheduleCancel() {
        Verifier verifier = new Verifier("OK");
        ScheduledFuture<String> future = scheduler.schedule((Callable) verifier, 50, TimeUnit.MILLISECONDS);
        future.cancel(false);
        assert scheduler.start().awaitIdling();
        assert verifyCanceled(future);
        assert verifier.verifyExecutionCount(0);
    }

    @RepeatedTest(5)
    void scheduleTaskAfterCancel() {
        Verifier verifier = new Verifier("OK");
        ScheduledFuture<String> future = scheduler.schedule((Callable) verifier, 50, TimeUnit.MILLISECONDS);
        future.cancel(false);
        assert scheduler.start().awaitIdling();
        assert verifyCanceled(future);
        assert verifier.verifyExecutionCount(0);

        // reschedule
        ScheduledFuture<String> reFuture = scheduler.schedule((Callable) verifier, 50, TimeUnit.MILLISECONDS);
        assert verifyRunning(reFuture);
        assert scheduler.awaitIdling();
        assert verifySuccessed(reFuture, "OK");
        assert verifier.verifyExecutionCount(1);
    }

    @RepeatedTest(5)
    void fixedRate() {
        Verifier verifier = new Verifier();
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(verifier, 0, 50, TimeUnit.MILLISECONDS);

        assert verifyRunning(future);
        assert scheduler.start().awaitExecutions(3);
        assert verifier.verifyExecutionCount(3);
        assert verifier.verifyRate(0, 30, 30);
    }

    @RepeatedTest(5)
    void fixedDelay() {
        Verifier verifier = new Verifier();
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(verifier, 0, 50, TimeUnit.MILLISECONDS);

        assert verifyRunning(future);
        assert scheduler.start().awaitExecutions(3);
        assert verifier.verifyExecutionCount(3);
        assert verifier.verifyInterval(0, 50, 50);
    }

    @Test
    void cron() {
        scheduler.limitAwaitTime(4000);

        Verifier verifier = new Verifier();
        ScheduledFuture<?> future = scheduler.scheduleAt(verifier, "* * * * * *");

        assert verifyRunning(future);
        assert scheduler.start().awaitExecutions(3);
        assert verifier.verifyExecutionCount(3);
        assert verifier.verifyRate(0, 1000, 1000);
    }

    @Test
    void cronStep() {
        scheduler.limitAwaitTime(4000);

        Verifier verifier = new Verifier();
        ScheduledFuture<?> future = scheduler.scheduleAt(verifier, "*/2 * * * * *");

        assert verifyRunning(future);
        assert scheduler.start().awaitExecutions(2);
        assert verifier.verifyExecutionCount(2);
        assert verifier.verifyInterval(0, 2000);
    }

    void testInvokeAll() throws InterruptedException {
        List<Callable<Integer>> tasks = List.of(() -> 1, () -> 2, () -> 3);
        List<Future<Integer>> futures = scheduler.invokeAll(tasks);
        assertEquals(3, futures.size());
        assertAll(() -> assertEquals(1, futures.get(0).get()), () -> assertEquals(2, futures.get(1)
                .get()), () -> assertEquals(3, futures.get(2).get()));
    }

    @RepeatedTest(5)
    void shutdown() throws InterruptedException {
        Verifier verifier = new Verifier();
        ScheduledFuture<String> future = scheduler.schedule((Callable) verifier, 100, TimeUnit.MILLISECONDS);
        assert scheduler.isShutdown() == false;
        assert scheduler.isTerminated() == false;
        assert verifyRunning(future);

        assert scheduler.start().awaitIdling();
        scheduler.shutdown();
        assert scheduler.isShutdown() == true;
        assertTrue(scheduler.awaitTermination(1, TimeUnit.SECONDS));
        assert scheduler.isTerminated() == true;
    }

    void testShutdownNow() {
        scheduler.schedule(() -> {
        }, 1, TimeUnit.HOURS); // Schedule a task far in the future
        List<Runnable> pendingTasks = scheduler.shutdownNow();
        assertFalse(pendingTasks.isEmpty());
        assertTrue(scheduler.isShutdown());
    }

    @RepeatedTest(5)
    void handleExceptionDuringTask() {
        Verifier verifier = new Verifier(new Error("Fail"));
        ScheduledFuture<?> future = scheduler.schedule((Callable) verifier, 50, TimeUnit.MILLISECONDS);
        assert verifyRunning(future);
        assert scheduler.start().awaitIdling();
        assert verifyFailed(future);
        assert verifier.verifyExecutionCount(1);
    }
}