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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

class CronTest {

    TimeZone original;

    ZoneId zoneId = ZoneId.systemDefault();

    @Test
    public void invalidLength() {
        assertThrows(IllegalArgumentException.class, () -> new Parsed(""));
        assertThrows(IllegalArgumentException.class, () -> new Parsed("*"));
        assertThrows(IllegalArgumentException.class, () -> new Parsed("* *"));
        assertThrows(IllegalArgumentException.class, () -> new Parsed("* * *"));
        assertThrows(IllegalArgumentException.class, () -> new Parsed("* * * *"));
        assertThrows(IllegalArgumentException.class, () -> new Parsed("* * * * * * *"));
        assertThrows(IllegalArgumentException.class, () -> new Parsed("* * * * * * * *"));
    }

    @Test
    public void whitespace() {
        assert new Parsed("* * * * * * ").next("2024-10-02T00:00:00", "2024-10-02T00:00:01");
        assert new Parsed(" * * * * * *").next("2024-10-02T00:00:00", "2024-10-02T00:00:01");
        assert new Parsed(" * * * * * * ").next("2024-10-02T00:00:00", "2024-10-02T00:00:01");
        assert new Parsed("     *    * *       *      *         *      ").next("2024-10-02T00:00:00", "2024-10-02T00:00:01");
    }

    @Test
    public void ignoreField() {
        assert new Parsed("* * ? * 3").next("2024-10-08T10:20:30", "2024-10-09T00:00:00");
        assert new Parsed("* * 10 * ?").next("2024-10-08T10:20:30", "2024-10-10T00:00:00");

        assertThrows(IllegalArgumentException.class, () -> new Parsed("? * * * * *"));
        assertThrows(IllegalArgumentException.class, () -> new Parsed("* ? * * * *"));
        assertThrows(IllegalArgumentException.class, () -> new Parsed("* * ? * * *"));
        assertThrows(IllegalArgumentException.class, () -> new Parsed("* * * * ? *"));
    }

    @Test
    public void lastDayOfMonth() {
        Parsed parsed = new Parsed("* * L * *");
        assert parsed.next("2024-10-08T10:20:30", "2024-10-31T00:00:00");
        assert parsed.next("2024-10-30T10:20:30", "2024-10-31T00:00:00");
        assert parsed.next("2024-10-31T10:20:30", "2024-10-31T10:21:00");
        assert parsed.next("2024-10-31T23:58:00", "2024-10-31T23:59:00");
        assert parsed.next("2024-10-31T23:59:59", "2024-11-30T00:00:00");
        assert parsed.next("2024-02-28T23:59:59", "2024-02-29T00:00:00"); // leap
        assert parsed.next("2024-02-29T23:59:59", "2024-03-31T00:00:00"); // leap
    }

    @Test
    public void all() {
        Parsed cronExpr = new Parsed("* * * * * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 1, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 10, 13, 0, 2, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 2, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 2, 1, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 59, 59, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 14, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void invalidInput() {
        assertThrows(NullPointerException.class, () -> new Parsed(null));
    }

    @Test
    public void secondNumber() {
        Parsed cronExpr = new Parsed("3 * * * * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 1, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 10, 13, 1, 3, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 1, 3, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 2, 3, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 59, 3, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 14, 0, 3, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 23, 59, 3, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 11, 0, 0, 3, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 30, 23, 59, 3, 0, zoneId);
        expected = ZonedDateTime.of(2012, 5, 1, 0, 0, 3, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void secondIncrement() {
        Parsed cronExpr = new Parsed("5/15 * * * * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 10, 13, 0, 5, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 0, 5, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 0, 20, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 0, 20, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 0, 35, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 0, 35, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 0, 50, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 0, 50, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 1, 5, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        // if rolling over minute then reset second (cron rules - increment affects only values in
        // own field)
        after = ZonedDateTime.of(2012, 4, 10, 13, 0, 50, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 1, 10, 0, zoneId);
        assert new Parsed("10/100 * * * * *").next(after).equals(expected);

        after = ZonedDateTime.of(2012, 4, 10, 13, 1, 10, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 2, 10, 0, zoneId);
        assert new Parsed("10/100 * * * * *").next(after).equals(expected);
    }

    @Test
    public void secondList() {
        Parsed cronExpr = new Parsed("7,19 * * * * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 10, 13, 0, 7, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 0, 7, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 0, 19, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 0, 19, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 1, 7, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void secondRange() {
        Parsed cronExpr = new Parsed("42-45 * * * * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 10, 13, 0, 42, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 0, 42, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 0, 43, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 0, 43, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 0, 44, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 0, 44, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 0, 45, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 0, 45, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 1, 42, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void secondInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> new Parsed("42-63 * * * * *"));
    }

    @Test
    public void secondInvalidIncrementModifier() {
        assertThrows(IllegalArgumentException.class, () -> new Parsed("42#3 * * * * *"));
    }

    @Test
    public void minuteNumber() {
        Parsed parsed = new Parsed("3 * * * *");
        assert parsed.next("10:01", "10:03");
        assert parsed.next("10:02", "10:03");
        assert parsed.next("10:03", "11:03");
        assert parsed.next("10:04", "11:03");
        assert parsed.next("10:55", "11:03");
        assert parsed.next("11:56", "12:03");
        assert parsed.next("2024-10-10T23:59", "2024-10-11T00:03");
        assert parsed.next("2024-12-31T23:59", "2025-01-01T00:03");
    }

    @Test
    public void minuteIncrement() {
        Parsed parsed = new Parsed("0/15 * * * *");
        assert parsed.next("10:01", "10:15");
        assert parsed.next("10:02", "10:15");
        assert parsed.next("10:14", "10:15");
        assert parsed.next("10:15", "10:30");
        assert parsed.next("10:16", "10:30");
        assert parsed.next("10:29", "10:30");
        assert parsed.next("10:30", "10:45");
        assert parsed.next("10:31", "10:45");
        assert parsed.next("10:44", "10:45");
        assert parsed.next("10:45", "11:00");
        assert parsed.next("10:46", "11:00");
        assert parsed.next("10:59", "11:00");
        assert parsed.next("11:00", "11:15");
        assert parsed.next("11:56", "12:00");
        assert parsed.next("2024-10-10T23:59", "2024-10-11T00:00");
        assert parsed.next("2024-12-31T23:59", "2025-01-01T00:00");
    }

    @Test
    public void minuteList() {
        Parsed parsed = new Parsed("3,10,22 * * * *");
        assert parsed.next("10:01", "10:03");
        assert parsed.next("10:02", "10:03");
        assert parsed.next("10:03", "10:10");
        assert parsed.next("10:04", "10:10");
        assert parsed.next("10:06", "10:10");
        assert parsed.next("10:08", "10:10");
        assert parsed.next("10:10", "10:22");
        assert parsed.next("10:12", "10:22");
        assert parsed.next("10:20", "10:22");
        assert parsed.next("11:56", "12:03");
        assert parsed.next("2024-10-10T23:59", "2024-10-11T00:03");
        assert parsed.next("2024-12-31T23:59", "2025-01-01T00:03");
    }

    @Test
    public void minuteRange() {
        Parsed parsed = new Parsed("3-10 * * * *");
        assert parsed.next("10:01", "10:03");
        assert parsed.next("10:02", "10:03");
        assert parsed.next("10:03", "10:04");
        assert parsed.next("10:04", "10:05");
        assert parsed.next("10:06", "10:07");
        assert parsed.next("10:08", "10:09");
        assert parsed.next("10:10", "11:03");
        assert parsed.next("11:56", "12:03");
        assert parsed.next("2024-10-10T23:59", "2024-10-11T00:03");
        assert parsed.next("2024-12-31T23:59", "2025-01-01T00:03");
    }

    @Test
    public void minuteRangeIncrement() {
        Parsed parsed = new Parsed("3-20/3 * * * *");
        assert parsed.next("10:02", "10:03");
        assert parsed.next("10:03", "10:06");
        assert parsed.next("10:04", "10:06");
        assert parsed.next("10:06", "10:09");
        assert parsed.next("10:08", "10:09");
        assert parsed.next("10:10", "10:12");
        assert parsed.next("10:22", "11:03");
        assert parsed.next("2024-10-10T23:59", "2024-10-11T00:03");
        assert parsed.next("2024-12-31T23:59", "2025-01-01T00:03");
    }

    @Test
    public void hourNumber() {
        Parsed cronExpr = new Parsed("0 * 3 * * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 1, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 11, 3, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 11, 3, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 11, 3, 1, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 11, 3, 59, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 12, 3, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void hourIncrement() {
        Parsed cronExpr = new Parsed("0 * 0/15 * * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 10, 15, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 15, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 15, 1, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 15, 59, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 11, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 11, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 11, 0, 1, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 11, 15, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 11, 15, 1, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void hourList() {
        Parsed cronExpr = new Parsed("0 * 7,19 * * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 10, 19, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 19, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 19, 1, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 19, 59, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 11, 7, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void dayOfMonthNumber() {
        Parsed cronExpr = new Parsed("0 * * 3 * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 5, 3, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 5, 3, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 5, 3, 0, 1, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 5, 3, 0, 59, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 5, 3, 1, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 5, 3, 23, 59, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 6, 3, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void dayOfMonthIncrement() {
        Parsed cronExpr = new Parsed("0 0 0 1/15 * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 16, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 16, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 5, 1, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 30, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 5, 1, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 5, 16, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void dayOfMonthList() {
        Parsed cronExpr = new Parsed("0 0 0 7,19 * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 19, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 19, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 5, 7, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 5, 7, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 5, 19, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 5, 30, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 6, 7, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void dayOfMonthLast() {
        Parsed cronExpr = new Parsed("0 0 0 L * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 30, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 2, 12, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 2, 29, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void dayOfMonthNumberLast_L() {
        Parsed cronExpr = new Parsed("0 0 0 3L * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 30 - 3, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 2, 12, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 2, 29 - 3, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void dayOfMonthClosestWeekdayW() {
        Parsed cronExpr = new Parsed("0 0 0 9W * *");

        // 9 - is weekday in may
        ZonedDateTime after = ZonedDateTime.of(2012, 5, 2, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 5, 9, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        // 9 - is weekday in may
        after = ZonedDateTime.of(2012, 5, 8, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        // 9 - saturday, friday closest weekday in june
        after = ZonedDateTime.of(2012, 5, 9, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 6, 8, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        // 9 - sunday, monday closest weekday in september
        after = ZonedDateTime.of(2012, 9, 1, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 9, 10, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void dayOfMonthInvalidModifier() {
        assertThrows(IllegalArgumentException.class, () -> new Parsed("0 0 0 9X * *"));
    }

    @Test
    public void dayOfMonthInvalidIncrementModifier() {
        assertThrows(IllegalArgumentException.class, () -> new Parsed("0 0 0 9#2 * *"));
    }

    @Test
    public void monthNumber() {
        ZonedDateTime after = ZonedDateTime.of(2012, 2, 12, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 5, 1, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 1 5 *").next(after).equals(expected);
    }

    @Test
    public void monthIncrement() {
        ZonedDateTime after = ZonedDateTime.of(2012, 2, 12, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 5, 1, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 1 5/2 *").next(after).equals(expected);

        after = ZonedDateTime.of(2012, 5, 1, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 7, 1, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 1 5/2 *").next(after).equals(expected);

        // if rolling over year then reset month field (cron rules - increments only affect own
        // field)
        after = ZonedDateTime.of(2012, 5, 1, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2013, 5, 1, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 1 5/10 *").next(after).equals(expected);
    }

    @Test
    public void monthList() {
        Parsed cronExpr = new Parsed("0 0 0 1 3,7,12 *");

        ZonedDateTime after = ZonedDateTime.of(2012, 2, 12, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 3, 1, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 3, 1, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 7, 1, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 7, 1, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 12, 1, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void monthListByName() {
        Parsed cronExpr = new Parsed("0 0 0 1 MAR,JUL,DEC *");

        ZonedDateTime after = ZonedDateTime.of(2012, 2, 12, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 3, 1, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 3, 1, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 7, 1, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 7, 1, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 12, 1, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void monthInvalidModifier() {
        assertThrows(IllegalArgumentException.class, () -> new Parsed("0 0 0 1 ? *"));
    }

    @Test
    public void dowNumber() {
        Parsed parsed = new Parsed("0 0 0 * * 1");
        assert parsed.next("2024-10-10", "2024-10-14");
        assert parsed.next("2024-10-11", "2024-10-14");
        assert parsed.next("2024-10-12", "2024-10-14");
        assert parsed.next("2024-10-13", "2024-10-14");
        assert parsed.next("2024-10-14", "2024-10-21");
        assert parsed.next("2024-10-15", "2024-10-21");
        assert parsed.next("2024-10-16", "2024-10-21");
        assert parsed.next("2024-10-17", "2024-10-21");
        assert parsed.next("2024-10-18", "2024-10-21");

        parsed = new Parsed("0 0 0 * * 3");
        assert parsed.next("2024-10-10", "2024-10-16");
        assert parsed.next("2024-10-11", "2024-10-16");
        assert parsed.next("2024-10-12", "2024-10-16");
        assert parsed.next("2024-10-13", "2024-10-16");
        assert parsed.next("2024-10-14", "2024-10-16");
        assert parsed.next("2024-10-15", "2024-10-16");
        assert parsed.next("2024-10-16", "2024-10-23");
        assert parsed.next("2024-10-17", "2024-10-23");
        assert parsed.next("2024-10-18", "2024-10-23");

        parsed = new Parsed("0 0 0 * * 5");
        assert parsed.next("2024-10-10", "2024-10-11");
        assert parsed.next("2024-10-11", "2024-10-18");
        assert parsed.next("2024-10-12", "2024-10-18");
        assert parsed.next("2024-10-13", "2024-10-18");
        assert parsed.next("2024-10-14", "2024-10-18");
        assert parsed.next("2024-10-15", "2024-10-18");
        assert parsed.next("2024-10-16", "2024-10-18");
        assert parsed.next("2024-10-17", "2024-10-18");
        assert parsed.next("2024-10-18", "2024-10-25");
    }

    @Test
    public void dowNumberZero() {
        Parsed parsed = new Parsed("0 0 0 * * 0");
        assert parsed.next("2024-10-10", "2024-10-13");
        assert parsed.next("2024-10-11", "2024-10-13");
        assert parsed.next("2024-10-12", "2024-10-13");
        assert parsed.next("2024-10-13", "2024-10-20");
        assert parsed.next("2024-10-14", "2024-10-20");
        assert parsed.next("2024-10-15", "2024-10-20");
        assert parsed.next("2024-10-16", "2024-10-20");
        assert parsed.next("2024-10-17", "2024-10-20");
        assert parsed.next("2024-10-18", "2024-10-20");
    }

    @Test
    public void dowIncrement() {
        Parsed parsed = new Parsed("0 0 0 * * 0/2");
        assert parsed.next("2024-10-10", "2024-10-13");
        assert parsed.next("2024-10-11", "2024-10-13");
        assert parsed.next("2024-10-12", "2024-10-13");
        assert parsed.next("2024-10-13", "2024-10-20");
        assert parsed.next("2024-10-14", "2024-10-20");
        assert parsed.next("2024-10-15", "2024-10-20");
        assert parsed.next("2024-10-16", "2024-10-20");
        assert parsed.next("2024-10-17", "2024-10-20");
        assert parsed.next("2024-10-18", "2024-10-20");

        parsed = new Parsed("0 0 0 * * 1/2");
        assert parsed.next("2024-10-10", "2024-10-11");
        assert parsed.next("2024-10-11", "2024-10-13");
        assert parsed.next("2024-10-12", "2024-10-13");
        assert parsed.next("2024-10-13", "2024-10-14");
        assert parsed.next("2024-10-14", "2024-10-16");
        assert parsed.next("2024-10-15", "2024-10-16");
        assert parsed.next("2024-10-16", "2024-10-18");
        assert parsed.next("2024-10-17", "2024-10-18");
        assert parsed.next("2024-10-18", "2024-10-20");

        parsed = new Parsed("0 0 0 * * 3/2");
        assert parsed.next("2024-10-10", "2024-10-11");
        assert parsed.next("2024-10-11", "2024-10-13");
        assert parsed.next("2024-10-12", "2024-10-13");
        assert parsed.next("2024-10-13", "2024-10-16");
        assert parsed.next("2024-10-14", "2024-10-16");
        assert parsed.next("2024-10-15", "2024-10-16");
        assert parsed.next("2024-10-16", "2024-10-18");
        assert parsed.next("2024-10-17", "2024-10-18");
        assert parsed.next("2024-10-18", "2024-10-20");
    }

    @Test
    public void dowStartIncrement() {
        Parsed parsed = new Parsed("0 0 0 * * */2");
        assert parsed.next("2024-10-07", "2024-10-09");
        assert parsed.next("2024-10-08", "2024-10-09");
        assert parsed.next("2024-10-09", "2024-10-11");
        assert parsed.next("2024-10-10", "2024-10-11");
        assert parsed.next("2024-10-11", "2024-10-13");
        assert parsed.next("2024-10-12", "2024-10-13");
        assert parsed.next("2024-10-13", "2024-10-14");
    }

    @Test
    public void dowListNum() {
        Parsed parsed = new Parsed("0 0 0 * * 1,2,3");
        assert parsed.next("2024-10-10", "2024-10-14");
        assert parsed.next("2024-10-11", "2024-10-14");
        assert parsed.next("2024-10-12", "2024-10-14");
        assert parsed.next("2024-10-13", "2024-10-14");
        assert parsed.next("2024-10-14", "2024-10-15");
        assert parsed.next("2024-10-15", "2024-10-16");
        assert parsed.next("2024-10-16", "2024-10-21");
        assert parsed.next("2024-10-17", "2024-10-21");
        assert parsed.next("2024-10-18", "2024-10-21");
    }

    @Test
    public void dowListNumUnsorted() {
        Parsed parsed = new Parsed("0 0 0 * * 3,2,1");
        assert parsed.next("2024-10-10", "2024-10-14");
        assert parsed.next("2024-10-11", "2024-10-14");
        assert parsed.next("2024-10-12", "2024-10-14");
        assert parsed.next("2024-10-13", "2024-10-14");
        assert parsed.next("2024-10-14", "2024-10-15");
        assert parsed.next("2024-10-15", "2024-10-16");
        assert parsed.next("2024-10-16", "2024-10-21");
        assert parsed.next("2024-10-17", "2024-10-21");
        assert parsed.next("2024-10-18", "2024-10-21");
    }

    @Test
    public void dowListName() {
        Parsed parsed = new Parsed("0 0 0 * * MON,TUE,WED");
        assert parsed.next("2024-10-10", "2024-10-14");
        assert parsed.next("2024-10-11", "2024-10-14");
        assert parsed.next("2024-10-12", "2024-10-14");
        assert parsed.next("2024-10-13", "2024-10-14");
        assert parsed.next("2024-10-14", "2024-10-15");
        assert parsed.next("2024-10-15", "2024-10-16");
        assert parsed.next("2024-10-16", "2024-10-21");
        assert parsed.next("2024-10-17", "2024-10-21");
        assert parsed.next("2024-10-18", "2024-10-21");
    }

    @Test
    public void dowListNameUnsorted() {
        Parsed parsed = new Parsed("0 0 0 * * WED,TUE,MON");
        assert parsed.next("2024-10-10", "2024-10-14");
        assert parsed.next("2024-10-11", "2024-10-14");
        assert parsed.next("2024-10-12", "2024-10-14");
        assert parsed.next("2024-10-13", "2024-10-14");
        assert parsed.next("2024-10-14", "2024-10-15");
        assert parsed.next("2024-10-15", "2024-10-16");
        assert parsed.next("2024-10-16", "2024-10-21");
        assert parsed.next("2024-10-17", "2024-10-21");
        assert parsed.next("2024-10-18", "2024-10-21");
    }

    @Test
    public void dowLastFridayInMonth() {
        Parsed cronExpr = new Parsed("0 0 0 * * 5L");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 1, 1, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 27, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 27, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 5, 25, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 2, 6, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 2, 24, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 2, 6, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 2, 24, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * FRIL").next(after).equals(expected);
    }

    @Test
    public void dowInterpret0Sunday() {
        ZonedDateTime after = ZonedDateTime.of(2012, 4, 1, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 8, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * 0").next(after).equals(expected);

        expected = ZonedDateTime.of(2012, 4, 29, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * 0L").next(after).equals(expected);

        expected = ZonedDateTime.of(2012, 4, 8, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * 0#2").next(after).equals(expected);
    }

    @Test
    public void dowInterpret7sunday() {
        ZonedDateTime after = ZonedDateTime.of(2012, 4, 1, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 8, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * 7").next(after).equals(expected);

        expected = ZonedDateTime.of(2012, 4, 29, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * 7L").next(after).equals(expected);

        expected = ZonedDateTime.of(2012, 4, 8, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * 7#2").next(after).equals(expected);
    }

    @Test
    public void dowNthDayInMonth() {
        ZonedDateTime after = ZonedDateTime.of(2012, 4, 1, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 20, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * 5#3").next(after).equals(expected);

        after = ZonedDateTime.of(2012, 4, 20, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 5, 18, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * 5#3").next(after).equals(expected);

        after = ZonedDateTime.of(2012, 3, 30, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 1, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * 7#1").next(after).equals(expected);

        after = ZonedDateTime.of(2012, 4, 1, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 5, 6, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * 7#1").next(after).equals(expected);

        after = ZonedDateTime.of(2012, 2, 6, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 2, 29, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * 3#5").next(after).equals(expected); // leapday

        after = ZonedDateTime.of(2012, 2, 6, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 2, 29, 0, 0, 0, 0, zoneId);
        assert new Parsed("0 0 0 * * WED#5").next(after).equals(expected); // leapday
    }

    @Test
    public void dowInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new Parsed("0 0 0 * * 5W"));
        assertThrows(IllegalArgumentException.class, () -> new Parsed("0 0 0 * * 5?3"));
        assertThrows(IllegalArgumentException.class, () -> new Parsed("0 0 0 * * 5*3"));
        assertThrows(IllegalArgumentException.class, () -> new Parsed("0 0 0 * * 12"));
    }

    @Test
    public void notSupportRollingPeriod() {
        assertThrows(IllegalArgumentException.class, () -> new Parsed("* * 5-1 * * *"));
    }

    @Test
    public void non_existing_date_throws_exception() {
        // Will check for the next 4 years - no 30th of February is found so a IAE is thrown.
        assertThrows(IllegalArgumentException.class, () -> new Parsed("* * * 30 2 *").next(ZonedDateTime.now()));
    }

    @Test
    public void defaultBarrier() {
        Parsed cronExpr = new Parsed("* * * 29 2 *");

        ZonedDateTime after = ZonedDateTime.of(2012, 3, 1, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2016, 2, 29, 0, 0, 0, 0, zoneId);
        // the default barrier is 4 years - so leap years are considered.
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void withoutSeconds() {
        ZonedDateTime after = ZonedDateTime.of(2012, 3, 1, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2016, 2, 29, 0, 0, 0, 0, zoneId);
        assert new Parsed("* * 29 2 *").next(after).equals(expected);
    }

    @Test
    public void triggerProblemSameMonth() {
        assertEquals(ZonedDateTime.parse("2020-01-02T00:50:00Z"), new Parsed("00 50 * 1-8 1 *")
                .next(ZonedDateTime.parse("2020-01-01T23:50:00Z")));
    }

    @Test
    public void triggerProblemNextMonth() {
        assertEquals(ZonedDateTime.parse("2020-02-01T00:50:00Z"), new Parsed("00 50 * 1-8 2 *")
                .next(ZonedDateTime.parse("2020-01-31T23:50:00Z")));
    }

    @Test
    public void triggerProblemNextYear() {
        assertEquals(ZonedDateTime.parse("2020-01-01T00:50:00Z"), new Parsed("00 50 * 1-8 1 *")
                .next(ZonedDateTime.parse("2019-12-31T23:50:00Z")));
    }

    @Test
    public void triggerProblemNextMonthMonthAst() {
        assertEquals(ZonedDateTime.parse("2020-02-01T00:50:00Z"), new Parsed("00 50 * 1-8 * *")
                .next(ZonedDateTime.parse("2020-01-31T23:50:00Z")));
    }

    @Test
    public void triggerProblemNextYearMonthAst() {
        assertEquals(ZonedDateTime.parse("2020-01-01T00:50:00Z"), new Parsed("00 50 * 1-8 * *")
                .next(ZonedDateTime.parse("2019-12-31T23:50:00Z")));
    }

    @Test
    public void triggerProblemNextMonthDayAst() {
        assertEquals(ZonedDateTime.parse("2020-02-01T00:50:00Z"), new Parsed("00 50 * * 2 *")
                .next(ZonedDateTime.parse("2020-01-31T23:50:00Z")));
    }

    @Test
    public void triggerProblemNextYearDayAst() {
        assertEquals(ZonedDateTime.parse("2020-01-01T00:50:00Z"), new Parsed("00 50 * * 1 *")
                .next(ZonedDateTime.parse("2019-12-31T22:50:00Z")));
    }

    @Test
    public void triggerProblemNextMonthAllAst() {
        assertEquals(ZonedDateTime.parse("2020-02-01T00:50:00Z"), new Parsed("00 50 * * * *")
                .next(ZonedDateTime.parse("2020-01-31T23:50:00Z")));
    }

    @Test
    public void triggerProblemNextYearAllAst() {
        assertEquals(ZonedDateTime.parse("2020-01-01T00:50:00Z"), new Parsed("00 50 * * * *")
                .next(ZonedDateTime.parse("2019-12-31T23:50:00Z")));
    }

    private static class Parsed {
        Cron[] fields;

        Parsed(String format) {
            fields = Scheduler.parse(format);
        }

        ZonedDateTime next(ZonedDateTime base) {
            return Scheduler.next(fields, base);
        }

        boolean next(String base, String expectedNext) {
            ZonedDateTime baseDate = parse(base);
            ZonedDateTime nextDate = parse(expectedNext);

            assert next(baseDate).isEqual(nextDate) : base + "   " + nextDate;
            return true;
        }

        private ZonedDateTime parse(String date) {
            if (date.indexOf('T') == -1) {
                if (date.indexOf(':') == -1) {
                    return LocalDate.parse(date).atTime(0, 0, 0).atZone(ZoneId.systemDefault());
                } else {
                    return LocalTime.parse(date).atDate(LocalDate.now()).atZone(ZoneId.systemDefault());
                }
            } else {
                return LocalDateTime.parse(date).atZone(ZoneId.systemDefault());
            }
        }
    }
}