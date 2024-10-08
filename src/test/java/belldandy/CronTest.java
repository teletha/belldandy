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

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CronTest {

    TimeZone original;

    ZoneId zoneId;

    @BeforeEach
    public void setup() {
        original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"));
        zoneId = TimeZone.getDefault().toZoneId();
    }

    @AfterEach
    public void cleanup() {
        TimeZone.setDefault(original);
    }

    private boolean matches(Cron field, int value) {
        return field.matches(ZonedDateTime.now().with(field.type.field, value));
    }

    private boolean matcheAll(Cron field, int... values) {
        for (int value : values) {
            assert matches(field, value);
        }
        return true;
    }

    private boolean unmatcheAll(Cron field, int... values) {
        for (int value : values) {
            assert matches(field, value) == false;
        }
        return true;
    }

    @Test
    public void parseNumber() {
        Cron field = new Cron(Cron.MINUTE, "5");
        assert matches(field, 5);
        assert unmatcheAll(field, 2, 4, 6, 8, 10, 30, 59);
    }

    @Test
    public void parseNumberWithIncrement() {
        Cron field = new Cron(Cron.MINUTE, "0/15");
        assert matcheAll(field, 0, 15, 30, 45);
        assert unmatcheAll(field, 1, 2, 3, 4, 6, 7, 8, 9, 11, 12, 20, 33, 59);
    }

    @Test
    public void parseRange() {
        Cron field = new Cron(Cron.MINUTE, "5-10");
        assert matcheAll(field, 5, 6, 7, 8, 9, 10);
        assert unmatcheAll(field, 1, 2, 3, 4, 11, 12, 30, 59);
    }

    @Test
    public void parseRangeWithIncrement() {
        Cron field = new Cron(Cron.MINUTE, "20-30/2");
        assert matcheAll(field, 20, 22, 24, 26, 28, 30);
        assert unmatcheAll(field, 18, 19, 21, 23, 25, 27, 29, 31, 32, 59);
    }

    @Test
    public void parseAsterisk() {
        Cron field = new Cron(Cron.DAY_OF_WEEK, "*");
        assert matcheAll(field, 1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void parseAsteriskWithIncrement() {
        Cron field = new Cron(Cron.DAY_OF_WEEK, "*/2");
        assert matcheAll(field, 1, 3, 5, 7);
        assert unmatcheAll(field, 2, 4, 6);
    }

    @Test
    public void ignoreFieldInDayOfWeek() {
        Cron field = new Cron(Cron.DAY_OF_WEEK, "?");
        assert field.matches(ZonedDateTime.now());
    }

    @Test
    public void ignoreFieldInDayOfMonth() {
        Cron field = new Cron(Cron.DAY_OF_MONTH, "?");
        assert field.matches(ZonedDateTime.now());
    }

    @Test
    public void giveErrorIfInvalidCountField() {
        assertThrows(IllegalArgumentException.class, () -> new Parsed("* 3 *"));
    }

    @Test
    public void giveErrorIfMinuteFieldIgnored() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Cron(Cron.MINUTE, "?");
        });
    }

    @Test
    public void giveErrorIfHourFieldIgnored() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Cron(Cron.HOUR, "?");
        });
    }

    @Test
    public void giveErrorIfMonthFieldIgnored() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Cron(Cron.MONTH, "?");
        });
    }

    @Test
    public void giveLastDayOfMonthInLeapYear() {
        Cron field = new Cron(Cron.DAY_OF_MONTH, "L");
        assert field.matches(ZonedDateTime.of(2012, 02, 29, 0, 0, 0, 0, ZoneId.systemDefault()));
    }

    @Test
    public void giveLastDayOfMonth() {
        Cron field = new Cron(Cron.DAY_OF_MONTH, "L");
        YearMonth now = YearMonth.now();
        assert field.matches(ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.lengthOfMonth(), 0, 0, 0, 0, ZoneId.systemDefault()));
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
        Parsed cronExpr = new Parsed("0 3 * * * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 1, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 10, 13, 3, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 3, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 14, 3, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void minuteIncrement() {
        Parsed cronExpr = new Parsed("0 0/15 * * * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 10, 13, 15, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 15, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 30, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 30, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 45, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 45, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 14, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void minuteList() {
        Parsed cronExpr = new Parsed("0 7,19 * * * *");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 10, 13, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 10, 13, 7, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 10, 13, 7, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 10, 13, 19, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
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
    public void hourRun25timesInDST_ChangeToWintertime() {
        Parsed cron = new Parsed("0 1 * * * *");
        ZonedDateTime start = ZonedDateTime.of(2011, 10, 30, 0, 0, 0, 0, zoneId);
        ZonedDateTime slutt = start.plusDays(1);
        ZonedDateTime tid = start;

        // throws: Unsupported unit: Seconds
        // assertEquals(25, Duration.between(start.toLocalDate(), slutt.toLocalDate()).toHours());

        int count = 0;
        ZonedDateTime lastTime = tid;
        while (tid.isBefore(slutt)) {
            ZonedDateTime nextTime = cron.next(tid);
            assert nextTime.isAfter(lastTime);
            lastTime = nextTime;
            tid = tid.plusHours(1);
            count++;
        }
        assertEquals(25, count);
    }

    @Test
    public void hourRun23timesInDST_ChangeToSummertime() {
        Parsed cron = new Parsed("0 0 * * * *");
        ZonedDateTime start = ZonedDateTime.of(2011, 03, 27, 1, 0, 0, 0, zoneId);
        ZonedDateTime slutt = start.plusDays(1);
        ZonedDateTime tid = start;

        // throws: Unsupported unit: Seconds
        // assertEquals(23, Duration.between(start.toLocalDate(), slutt.toLocalDate()).toHours());

        int count = 0;
        ZonedDateTime lastTime = tid;
        while (tid.isBefore(slutt)) {
            ZonedDateTime nextTime = cron.next(tid);
            assert nextTime.isAfter(lastTime);
            lastTime = nextTime;
            tid = tid.plusHours(1);
            count++;
        }
        assertEquals(23, count);
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
        Parsed cronExpr = new Parsed("0 0 0 * * 3");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 1, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 4, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 4, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 11, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 12, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 18, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 18, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 25, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void dowIncrement() {
        Parsed cronExpr = new Parsed("0 0 0 * * 3/2");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 1, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 4, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 4, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 6, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 6, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 8, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 8, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 11, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void dowList() {
        Parsed cronExpr = new Parsed("0 0 0 * * 1,5,7");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 1, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 2, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 2, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 6, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 6, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 8, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
    }

    @Test
    public void dowListName() {
        Parsed cronExpr = new Parsed("0 0 0 * * MON,FRI,SUN");

        ZonedDateTime after = ZonedDateTime.of(2012, 4, 1, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2012, 4, 2, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 2, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 6, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));

        after = ZonedDateTime.of(2012, 4, 6, 0, 0, 0, 0, zoneId);
        expected = ZonedDateTime.of(2012, 4, 8, 0, 0, 0, 0, zoneId);
        assertEquals(expected, cronExpr.next(after));
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
    public void dowInvalidModifier() {
        assertThrows(IllegalArgumentException.class, () -> new Parsed("0 0 0 * * 5W"));
    }

    @Test
    public void dowInvalidIncrementModifier() {
        assertThrows(IllegalArgumentException.class, () -> new Parsed("0 0 0 * * 5?3"));
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
    }
}