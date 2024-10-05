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

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Bench {
    private static class Task implements Runnable {

        private int c;

        /**
         * @param counter
         */
        public Task(int counter) {
            c = counter;
        }

        @Override
        public void run() {
            try {
                System.out.println(c);
            } catch (Exception e) {
            }
        }
    }

    public static void main(String args[]) throws Exception {
        Random random = new Random();
        Scheduler scheduler = new Scheduler();

        // Create 10, 000 platform threads
        for (int counter = 0; counter < 1000_000; ++counter) {
            scheduler.schedule(new Task(counter), random.nextLong(5000, 1000 * 90), TimeUnit.MILLISECONDS);
        }

        Thread.sleep(1000 * 90);
    }
}
