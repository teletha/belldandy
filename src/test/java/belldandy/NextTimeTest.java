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

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

public class NextTimeTest {

    private ZonedDateTime date(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.systemDefault());
    }

    @Test
    void minute() {
        assert Scheduler.next(date(2020, 1, 2, 3, 5), -1, -1, -1, 0).isEqual(date(2020, 1, 2, 4, 0));
        assert Scheduler.next(date(2020, 1, 2, 3, 5), -1, -1, -1, 5).isEqual(date(2020, 1, 2, 3, 5));
        assert Scheduler.next(date(2020, 1, 2, 3, 5), -1, -1, -1, 30).isEqual(date(2020, 1, 2, 3, 30));

        assert Scheduler.next(date(2020, 1, 2, 3, 5), -1, -1, -1, -1).isEqual(date(2020, 1, 2, 3, 5));
    }

    @Test
    void hour() {
        assert Scheduler.next(date(2020, 1, 2, 3, 5), -1, -1, 1, -1).isEqual(date(2020, 1, 3, 1, 5));
        assert Scheduler.next(date(2020, 1, 2, 3, 5), -1, -1, 3, -1).isEqual(date(2020, 1, 2, 3, 5));
        assert Scheduler.next(date(2020, 1, 2, 3, 5), -1, -1, 5, -1).isEqual(date(2020, 1, 2, 5, 5));

        assert Scheduler.next(date(2020, 1, 2, 3, 5), -1, -1, -1, -1).isEqual(date(2020, 1, 2, 3, 5));
    }

    @Test
    void day() {
        assert Scheduler.next(date(2020, 1, 6, 3, 5), -1, 2, -1, -1).isEqual(date(2020, 2, 2, 3, 5));
        assert Scheduler.next(date(2020, 1, 6, 3, 5), -1, 6, -1, -1).isEqual(date(2020, 1, 6, 3, 5));
        assert Scheduler.next(date(2020, 1, 6, 3, 5), -1, 10, -1, -1).isEqual(date(2020, 1, 10, 3, 5));
        assert Scheduler.next(date(2020, 2, 6, 3, 5), -1, 31, -1, -1).isEqual(date(2020, 3, 31, 3, 5));

        assert Scheduler.next(date(2020, 1, 6, 3, 5), -1, -1, -1, -1).isEqual(date(2020, 1, 6, 3, 5));
    }

    @Test
    void month() {
        assert Scheduler.next(date(2020, 5, 6, 3, 5), 1, -1, -1, -1).isEqual(date(2021, 1, 6, 3, 5));
        assert Scheduler.next(date(2020, 5, 6, 3, 5), 5, -1, -1, -1).isEqual(date(2020, 5, 6, 3, 5));
        assert Scheduler.next(date(2020, 5, 6, 3, 5), 12, -1, -1, -1).isEqual(date(2020, 12, 6, 3, 5));

        assert Scheduler.next(date(2020, 5, 6, 3, 5), -1, -1, -1, -1).isEqual(date(2020, 5, 6, 3, 5));
    }
}
