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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.RepeatedTest;

@SuppressWarnings("resource")
class VirtualSchedulerTest extends ExecutorTestSupport {

    @RepeatedTest(5)
    void execute() {
        int[] count = {0};
        executor.execute(() -> {
            count[0] = 1;
        });

        assert executor.start().awaitIdling();
        assert count[0] == 1 : Arrays.toString(count) + "  " + count[0] + executor;
    }

    @RepeatedTest(5)
    void submitCallable() {
        Verifier verifier = new Verifier("OK");
        Future<String> future = executor.submit((Callable) verifier);
        assert verifyRunning(future);
        assert executor.start().awaitIdling();
        assert verifySuccessed(future, "OK");
        assert verifier.verifyExecutionCount(1);
    }

    @RepeatedTest(5)
    void submitCallableCancel() {
        Verifier verifier = new Verifier("OK");
        Future<String> future = executor.submit((Callable) verifier);
        future.cancel(false);
        assert executor.start().awaitIdling();
        assert verifyCanceled(future);
        assert verifier.verifyExecutionCount(0);
    }

    @RepeatedTest(5)
    void submitRunnable() {
        int[] count = {0};
        Future<?> future = executor.submit((Runnable) () -> count[0]++);
        assert verifyRunning(future);
        assert executor.start().awaitIdling();
        assert verifySuccessed(future, null);
        assert count[0] == 1;
    }

    @RepeatedTest(5)
    void submitRunnableCancle() {
        int[] count = {0};
        Future<?> future = executor.submit((Runnable) () -> count[0]++);
        future.cancel(false);
        assert executor.start().awaitIdling();
        assert verifyCanceled(future);
    }

    @RepeatedTest(5)
    void schedule() {
        Verifier verifier = new Verifier("OK");
        ScheduledFuture<String> future = executor.schedule((Callable) verifier, 50, TimeUnit.MILLISECONDS);
        assert verifyRunning(future);
        assert executor.start().awaitIdling();
        assert verifySuccessed(future, "OK");
        assert verifier.verifyInitialDelay(50, TimeUnit.MILLISECONDS);
        assert verifier.verifyExecutionCount(1);
    }

    @RepeatedTest(5)
    void scheduleMultiSameDelay() {
        Verifier verifier1 = new Verifier("1");
        Verifier verifier2 = new Verifier("2");
        Verifier verifier3 = new Verifier("3");
        ScheduledFuture<String> future1 = executor.schedule((Callable) verifier1, 50, TimeUnit.MILLISECONDS);
        ScheduledFuture<String> future2 = executor.schedule((Callable) verifier2, 50, TimeUnit.MILLISECONDS);
        ScheduledFuture<String> future3 = executor.schedule((Callable) verifier3, 50, TimeUnit.MILLISECONDS);
        assert verifyRunning(future1, future2, future3);
        assert executor.start().awaitIdling();
        assert verifySuccessed(future1, "1");
        assert verifySuccessed(future2, "2");
        assert verifySuccessed(future3, "3");
        assert verifier1.verifyInitialDelay(50, TimeUnit.MILLISECONDS);
        assert verifier1.verifyExecutionCount(1);
        assert verifier2.verifyInitialDelay(50, TimeUnit.MILLISECONDS);
        assert verifier2.verifyExecutionCount(1);
        assert verifier3.verifyInitialDelay(50, TimeUnit.MILLISECONDS);
        assert verifier3.verifyExecutionCount(1);
    }

    @RepeatedTest(5)
    void scheduleMultiDifferentDelay() {
        Verifier verifier1 = new Verifier();
        Verifier verifier2 = new Verifier();
        Verifier verifier3 = new Verifier();
        ScheduledFuture<String> future1 = executor.schedule((Callable) verifier1, 100, TimeUnit.MILLISECONDS);
        ScheduledFuture<String> future2 = executor.schedule((Callable) verifier2, 50, TimeUnit.MILLISECONDS);
        ScheduledFuture<String> future3 = executor.schedule((Callable) verifier3, 20, TimeUnit.MILLISECONDS);
        assert verifyRunning(future1, future2, future3);
        assert executor.start().awaitIdling();
        assert verifySuccessed(future1, future2, future3);
        assert verifyExecutionOrder(verifier3, verifier2, verifier1);
    }

    @RepeatedTest(5)
    void scheduleCancel() {
        Verifier verifier = new Verifier("OK");
        ScheduledFuture<String> future = executor.schedule((Callable) verifier, 50, TimeUnit.MILLISECONDS);
        future.cancel(false);
        assert executor.start().awaitIdling();
        assert verifyCanceled(future);
        assert verifier.verifyExecutionCount(0);
    }

    @RepeatedTest(5)
    void scheduleTaskAfterCancel() {
        Verifier verifier = new Verifier("OK");
        ScheduledFuture<String> future = executor.schedule((Callable) verifier, 50, TimeUnit.MILLISECONDS);
        future.cancel(false);
        assert executor.start().awaitIdling();
        assert verifyCanceled(future);
        assert verifier.verifyExecutionCount(0);

        // reschedule
        ScheduledFuture<String> reFuture = executor.schedule((Callable) verifier, 50, TimeUnit.MILLISECONDS);
        assert verifyRunning(reFuture);
        assert executor.awaitIdling();
        assert verifySuccessed(reFuture, "OK");
        assert verifier.verifyExecutionCount(1);
    }

    @RepeatedTest(5)
    void fixedRate() {
        Verifier verifier = new Verifier().max(3);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(verifier, 0, 50, TimeUnit.MILLISECONDS);

        assert verifyRunning(future);
        assert executor.start().awaitIdling();
        assert verifyCanceled(future);
        assert verifier.verifyExecutionCount(3);
        assert verifier.verifyRate(TimeUnit.MILLISECONDS, 0, 30, 30);
    }

    @RepeatedTest(5)
    void fixedDelay() {
        Verifier verifier = new Verifier().max(3);
        ScheduledFuture<?> future = executor.scheduleWithFixedDelay(verifier, 0, 50, TimeUnit.MILLISECONDS);

        assert verifyRunning(future);
        assert executor.start().awaitIdling();
        assert verifyCanceled(future);
        assert verifier.verifyExecutionCount(3);
        assert verifier.verifyInterval(TimeUnit.MILLISECONDS, 0, 50, 50);
    }

    void testInvokeAll() throws InterruptedException {
        List<Callable<Integer>> tasks = List.of(() -> 1, () -> 2, () -> 3);
        List<Future<Integer>> futures = executor.invokeAll(tasks);
        assertEquals(3, futures.size());
        assertAll(() -> assertEquals(1, futures.get(0).get()), () -> assertEquals(2, futures.get(1)
                .get()), () -> assertEquals(3, futures.get(2).get()));
    }

    @RepeatedTest(5)
    void shutdown() throws InterruptedException {
        Verifier verifier = new Verifier();
        ScheduledFuture<String> future = executor.schedule((Callable) verifier, 100, TimeUnit.MILLISECONDS);
        assert executor.isShutdown() == false;
        assert executor.isTerminated() == false;
        assert verifyRunning(future);

        assert executor.start().awaitIdling();
        executor.shutdown();
        assert executor.isShutdown() == true;
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        assert executor.isTerminated() == true;
    }

    void testShutdownNow() {
        executor.schedule(() -> {
        }, 1, TimeUnit.HOURS); // Schedule a task far in the future
        List<Runnable> pendingTasks = executor.shutdownNow();
        assertFalse(pendingTasks.isEmpty());
        assertTrue(executor.isShutdown());
    }

    void testExceptionHandling() {
        ScheduledFuture<String> future = executor.schedule(() -> {
            throw new RuntimeException("Test exception");
        }, 100, TimeUnit.MILLISECONDS);

        ExecutionException exception = assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
        assertEquals("Test exception", exception.getCause().getMessage());
    }

    @RepeatedTest(5)
    void handleExceptionDuringTask() {
        Verifier verifier = new Verifier(new Error("Fail"));
        ScheduledFuture<?> future = executor.schedule((Callable) verifier, 50, TimeUnit.MILLISECONDS);
        assert verifyRunning(future);
        assert executor.start().awaitIdling();
        assert verifyFailed(future);
        assert verifier.verifyExecutionCount(1);
    }
}