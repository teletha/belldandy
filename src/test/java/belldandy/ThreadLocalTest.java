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
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class ThreadLocalTest extends SchedulerTestSupport {

    @Test
    void inherit() {
        InheritableThreadLocal<String> local = new InheritableThreadLocal();
        local.set("ROOT");

        Verifier<String> verifier = new Verifier(() -> local.get());
        Future future = scheduler.submit((Callable) verifier);

        assert scheduler.start().awaitIdling();
        assert verifySuccessed(future, "ROOT");
    }
}
