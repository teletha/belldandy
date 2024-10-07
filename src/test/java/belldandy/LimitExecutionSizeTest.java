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

import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
public class LimitExecutionSizeTest extends SchedulerTestSupport {

    @Test
    void limit() {
        scheduler = new TestableScheduler(1);

        Verifier<String> verifier1 = new Verifier(() -> {
            Thread.sleep(200);
            return "first";
        });
        Verifier<String> verifier2 = new Verifier("second");

        Future<String> future1 = scheduler.submit(verifier1.asCallable());
        Future<String> future2 = scheduler.submit(verifier2.asCallable());
        assert scheduler.start().awaitIdling();
        assert verifySuccessed(future1, "first");
        assert verifySuccessed(future2, "second");
        assert verifyExecutionOrder(verifier1, verifier2);
    }
}
