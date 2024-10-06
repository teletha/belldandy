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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.Future.State;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import kiss.I;
import kiss.WiseSupplier;

public class SchedulerTestSupport {

    protected static final int MULTIPLICITY = 10;

    private static final long TOLERANCE = 15;

    protected TestableScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TestableScheduler();

        assert !Thread.currentThread().isVirtual();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        scheduler.shutdown();
    }

    /**
     * Verify the given {@link Future} is running.
     * 
     * @param futures
     * @return
     */
    protected boolean verifyRunning(Future... futures) {
        for (Future future : futures) {
            assert future.isCancelled() == false;
            assert future.isDone() == false;
            assert future.state() == State.RUNNING;
        }
        return true;
    }

    /**
     * Verify the given {@link Future} is canceled.
     * 
     * @param futures
     * @return
     */
    protected boolean verifyCanceled(Future... futures) {
        for (Future future : futures) {
            assert future.isCancelled() == true;
            assert future.isDone() == true;
            assert future.state() == State.CANCELLED;
            assertThrows(CancellationException.class, () -> future.get());
        }
        return true;
    }

    /**
     * Verify the given {@link Future} is canceled.
     * 
     * @param futures
     * @return
     */
    protected boolean verifyFailed(Future... futures) {
        for (Future future : futures) {
            assert future.isCancelled() == false;
            assert future.isDone() == true;
            assert future.state() == State.FAILED;
        }
        return true;
    }

    /**
     * Verify the given {@link Future} is canceled.
     * 
     * @param futures
     * @return
     */
    protected <T> boolean verifySuccessed(Future<T>... futures) {
        for (Future<T> future : futures) {
            assert future.isCancelled() == false;
            assert future.isDone() == true;
            assert future.state() == State.SUCCESS;
        }
        return true;
    }

    /**
     * Verify the given {@link Future} is canceled.
     * 
     * @param future
     * @return
     */
    protected <T> boolean verifySuccessed(Future<T> future, T result) {
        try {
            assert future.isCancelled() == false;
            assert future.isDone() == true;
            assert future.state() == State.SUCCESS;
            assert Objects.equals(future.get(), result);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verify that they are executed in the specified order.
     * 
     * @return
     */
    protected boolean verifyExecutionOrder(Verifier<?>... verifiers) {
        assert verifyStartExecutionOrder(verifiers);
        assert verifyEndExecutionOrder(verifiers);
        return true;
    }

    /**
     * Verify that they are executed in the specified order.
     * 
     * @return
     */
    protected boolean verifyStartExecutionOrder(Verifier<?>... verifiers) {
        for (int i = 1; i < verifiers.length; i++) {
            assert verifiers[i - 1].startTime.getFirst() <= verifiers[i].startTime.getFirst();
        }
        return true;
    }

    /**
     * Verify that they are executed in the specified order.
     * 
     * @return
     */
    protected boolean verifyEndExecutionOrder(Verifier<?>... verifiers) {
        for (int i = 1; i < verifiers.length; i++) {
            assert verifiers[i - 1].endTime.getFirst() <= verifiers[i].endTime.getFirst();
        }
        return true;
    }

    /**
     * Verifiable {@link Callable} implementation.
     */
    protected class Verifier<T> implements Callable<T>, Runnable {

        private final long created = System.currentTimeMillis();

        private final List<Long> startTime = new ArrayList();

        private final List<Long> endTime = new ArrayList();

        private final Supplier<T> expectedResult;

        private final Throwable expectedError;

        public Verifier() {
            this((T) "Success");
        }

        public Verifier(T expectedResult) {
            this.expectedResult = () -> expectedResult;
            this.expectedError = null;
        }

        public Verifier(WiseSupplier<T> expectedResult) {
            this.expectedResult = expectedResult;
            this.expectedError = null;
        }

        public Verifier(Throwable error) {
            this.expectedResult = null;
            this.expectedError = error;
        }

        public Runnable asRunnable() {
            return this;
        }

        public Callable<T> asCallable() {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T call() throws Exception {
            startTime.add(System.currentTimeMillis());
            try {
                if (expectedError != null) {
                    throw I.quiet(expectedError);
                } else {
                    return expectedResult.get();
                }
            } finally {
                endTime.add(System.currentTimeMillis());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                call();
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }

        /**
         * Verify the initial delay.
         */
        protected boolean verifyInitialDelay(long millis) {
            assert !startTime.isEmpty();
            assert millis - TOLERANCE <= startTime.get(0) - created;

            return true;
        }

        /**
         * Verify rate.
         */
        protected boolean verifyRate(long... millis) {
            assert startTime.size() == millis.length;
            for (int i = 0; i < millis.length; i++) {
                long diff = startTime.get(i) - (i == 0 ? created : startTime.get(i - 1));
                assert millis[i] - TOLERANCE <= diff : diff;
            }
            return true;
        }

        /**
         * Verify interval.
         */
        protected boolean verifyInterval(long... millis) {
            assert startTime.size() == millis.length;
            for (int i = 0; i < millis.length; i++) {
                long diff = startTime.get(i) - (i == 0 ? created : endTime.get(i - 1));
                assert millis[i] - TOLERANCE <= diff : diff;
            }
            return true;
        }

        /**
         * Verify the execution count.
         */
        protected boolean verifyExecutionCount(long beforeAndAfter) {
            return verifyExecutionCount(beforeAndAfter, beforeAndAfter);
        }

        /**
         * Verify the execution count.
         */
        protected boolean verifyExecutionCount(long before, long after) {
            assert startTime.size() == before;
            assert endTime.size() == after;
            return true;
        }

        /**
         * Verify the execution count.
         */
        protected boolean verifyBeforeExecutionCount(long expected) {
            assert startTime.size() == expected;
            return true;
        }

        /**
         * Verify the execution count.
         */
        protected boolean verifyAfterExecutionCount(long expected) {
            assert endTime.size() == expected;
            return true;
        }
    }
}
