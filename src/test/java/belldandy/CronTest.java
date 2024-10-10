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
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

class CronTest {

    TimeZone original;

    ZoneId zoneId = ZoneId.systemDefault();

    private boolean invalidFormat(String expression) {
        assertThrows(IllegalArgumentException.class, () -> new Parsed(expression));
        return true;
    }

    @Test
    void invalidLength() {
        assert invalidFormat("");
        assert invalidFormat("*");
        assert invalidFormat("* *");
        assert invalidFormat("* * *");
        assert invalidFormat("* * * *");
        assert invalidFormat("* * * * * * *");
        assert invalidFormat("* * * * * * * *");
    }

    @Test
    void whitespace() {
        assert new Parsed("* * * * * * ").next("2024-10-02T00:00:00", "2024-10-02T00:00:01");
        assert new Parsed(" * * * * * *").next("2024-10-02T00:00:00", "2024-10-02T00:00:01");
        assert new Parsed(" * * * * * * ").next("2024-10-02T00:00:00", "2024-10-02T00:00:01");
        assert new Parsed("     *    * *       *      *         *      ").next("2024-10-02T00:00:00", "2024-10-02T00:00:01");
    }

    @Test
    void ignoreField() {
        assert new Parsed("* * ? * 3").next("2024-10-08T10:20:30", "2024-10-09T00:00:00");
        assert new Parsed("* * 10 * ?").next("2024-10-08T10:20:30", "2024-10-10T00:00:00");

        assert invalidFormat("? * * * * *");
        assert invalidFormat("* ? * * * *");
        assert invalidFormat("* * ? * * *");
        assert invalidFormat("* * * * ? *");
    }

    @Test
    void lastDayOfMonth() {
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
    void invalid() {
        assertThrows(NullPointerException.class, () -> new Parsed(null));
    }

    @Test
    void secondNumber() {
        Parsed parsed = new Parsed("10 * * * * *");
        assert parsed.next("10:20:{00~09}", "10:20:10");
        assert parsed.next("10:20:{10~59}", "10:21:10");

        parsed = new Parsed("0 0 0 29 2 *");
        assert parsed.next("2024-02-01", "2024-02-29");
        assert parsed.next("2025-02-01", "2028-02-29"); // leap
    }

    @Test
    void secondList() {
        Parsed parsed = new Parsed("10,20,30 * * * * *");
        assert parsed.next("10:20:{00~09}", "10:20:10");
        assert parsed.next("10:20:{10~19}", "10:20:20");
        assert parsed.next("10:20:{20~29}", "10:20:30");
        assert parsed.next("10:20:{30~59}", "10:21:10");
    }

    @Test
    void secondIncrement() {
        Parsed parsed = new Parsed("10/10 * * * * *");
        assert parsed.next("10:20:{00~09}", "10:20:10");
        assert parsed.next("10:20:{10~19}", "10:20:20");
        assert parsed.next("10:20:{20~29}", "10:20:30");
        assert parsed.next("10:20:{30~39}", "10:20:40");
        assert parsed.next("10:20:{40~49}", "10:20:50");
        assert parsed.next("10:20:{50~59}", "10:21:10");
    }

    @Test
    void secondIncrementList() {
        Parsed parsed = new Parsed("5/10,10/10 * * * * *");
        assert parsed.next("10:20:{00~04}", "10:20:05");
        assert parsed.next("10:20:{05~09}", "10:20:10");
        assert parsed.next("10:20:{20~24}", "10:20:25");
        assert parsed.next("10:20:{25~29}", "10:20:30");
        assert parsed.next("10:20:{30~34}", "10:20:35");
        assert parsed.next("10:20:{35~39}", "10:20:40");
        assert parsed.next("10:20:{40~44}", "10:20:45");
        assert parsed.next("10:20:{45~49}", "10:20:50");
    }

    @Test
    void secondIncrementListUnsorted() {
        Parsed parsed = new Parsed("10/10,5/10 * * * * *");
        assert parsed.next("10:20:{00~04}", "10:20:05");
        assert parsed.next("10:20:{05~09}", "10:20:10");
        assert parsed.next("10:20:{20~24}", "10:20:25");
        assert parsed.next("10:20:{25~29}", "10:20:30");
        assert parsed.next("10:20:{30~34}", "10:20:35");
        assert parsed.next("10:20:{35~39}", "10:20:40");
        assert parsed.next("10:20:{40~44}", "10:20:45");
        assert parsed.next("10:20:{45~49}", "10:20:50");
    }

    @Test
    void secondRange() {
        Parsed parsed = new Parsed("10-15 * * * * *");
        assert parsed.next("10:20:{00~09}", "10:20:10");
        assert parsed.next("10:20:10", "10:20:11");
        assert parsed.next("10:20:11", "10:20:12");
        assert parsed.next("10:20:12", "10:20:13");
        assert parsed.next("10:20:13", "10:20:14");
        assert parsed.next("10:20:14", "10:20:15");
        assert parsed.next("10:20:{15~59}", "10:21:10");
    }

    @Test
    void secondRangeList() {
        Parsed parsed = new Parsed("10-15,20-25 * * * * *");
        assert parsed.next("10:20:{00~09}", "10:20:10");
        assert parsed.next("10:20:10", "10:20:11");
        assert parsed.next("10:20:11", "10:20:12");
        assert parsed.next("10:20:{15~19}", "10:20:20");
        assert parsed.next("10:20:20", "10:20:21");
        assert parsed.next("10:20:21", "10:20:22");
        assert parsed.next("10:20:{25~59}", "10:21:10");
    }

    @Test
    void secondRangeListUnsorted() {
        Parsed parsed = new Parsed("20-25,10-15 * * * * *");
        assert parsed.next("10:20:{00~09}", "10:20:10");
        assert parsed.next("10:20:10", "10:20:11");
        assert parsed.next("10:20:11", "10:20:12");
        assert parsed.next("10:20:{15~19}", "10:20:20");
        assert parsed.next("10:20:20", "10:20:21");
        assert parsed.next("10:20:21", "10:20:22");
        assert parsed.next("10:20:{25~59}", "10:21:10");
    }

    @Test
    void secondInvalid() {
        assert invalidFormat("-1 * * * * *");
        assert invalidFormat("60 * * * * *");
        assert invalidFormat("100 * * * * *");
        assert invalidFormat("0.2 * * * * *");
        assert invalidFormat("# * * * * *");
        assert invalidFormat("@ * * * * *");
        assert invalidFormat("X * * * * *");

        assert invalidFormat("42-63 * * * * *");
        assert invalidFormat("42#3 * * * * *");
    }

    @Test
    void minuteNumber() {
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
    void minuteIncrement() {
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
    void minuteList() {
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
    void minuteRange() {
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
    void minuteRangeIncrement() {
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
    void minuteRangeIncrementOne() {
        Parsed parsed = new Parsed("3-20/1 * * * *");
        assert parsed.next("10:02", "10:03");
        assert parsed.next("10:03", "10:04");
        assert parsed.next("10:04", "10:05");
        assert parsed.next("10:06", "10:07");
        assert parsed.next("10:08", "10:09");
        assert parsed.next("10:10", "10:11");
        assert parsed.next("10:22", "11:03");
    }

    @Test
    void minuteInvalid() {
        assert invalidFormat("-1 * * * *");
        assert invalidFormat("60 * * * *");
        assert invalidFormat("100 * * * *");
        assert invalidFormat("0.2 * * * *");
        assert invalidFormat("# * * * *");
        assert invalidFormat("@ * * * *");
        assert invalidFormat("X * * * *");

        assert invalidFormat("42-63 * * * *");
        assert invalidFormat("42#3 * * * *");
    }

    @Test
    void hourNumber() {
        Parsed parsed = new Parsed("0 13 * * *");
        assert parsed.next("{00~12}:00", "13:00");
        assert parsed.next("2024-10-10T{13~23}:00:00", "2024-10-11T13:00:00");
        assert parsed.next("2024-10-11T{00~12}:00:00", "2024-10-11T13:00:00");
    }

    @Test
    void hourList() {
        Parsed parsed = new Parsed("0 13,16 * * *");
        assert parsed.next("{00~12}:00", "13:00");
        assert parsed.next("{13~15}:00", "16:00");
        assert parsed.next("2024-10-10T{16~23}:00:00", "2024-10-11T13:00:00");
        assert parsed.next("2024-10-11T{00~12}:00:00", "2024-10-11T13:00:00");
    }

    @Test
    void hourIncrement() {
        Parsed parsed = new Parsed("0 0/6 * * *");
        assert parsed.next("2024-10-10T{00~05}:00:00", "2024-10-10T06:00:00");
        assert parsed.next("2024-10-10T{06~11}:00:00", "2024-10-10T12:00:00");
        assert parsed.next("2024-10-10T{12~17}:00:00", "2024-10-10T18:00:00");
        assert parsed.next("2024-10-10T{18~23}:00:00", "2024-10-11T00:00:00");
        assert parsed.next("2024-10-11T{00~05}:00:00", "2024-10-11T06:00:00");
    }

    @Test
    void hourInvalid() {
        assert invalidFormat("* -1 * * *");
        assert invalidFormat("* 60 * * *");
        assert invalidFormat("* 100 * * *");
        assert invalidFormat("* 0.2 * * *");
        assert invalidFormat("* # * * *");
        assert invalidFormat("* @ * * *");
        assert invalidFormat("* X * * *");

        assert invalidFormat("* 42-63 * * *");
        assert invalidFormat("* 42#3 * * *");
    }

    @Test
    void dayNumber() {
        Parsed parsed = new Parsed("0 0 0 10 * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-{10~31}", "2024-11-10");

        parsed = new Parsed("0 0 0 29 2 *");
        assert parsed.next("2024-02-01", "2024-02-29");
        assert parsed.next("2025-02-01", "2028-02-29"); // leap
    }

    @Test
    void dayIncrement() {
        Parsed parsed = new Parsed("0 0 0 10/10 * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-{10~19}", "2024-10-20");
        assert parsed.next("2024-10-{20~29}", "2024-10-30");
        assert parsed.next("2024-10-{30~31}", "2024-11-10");
    }

    @Test
    void dayIncrementList() {
        Parsed parsed = new Parsed("0 0 0 10/10,15/10 * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-{10~14}", "2024-10-15");
        assert parsed.next("2024-10-{15~19}", "2024-10-20");
        assert parsed.next("2024-10-{20~24}", "2024-10-25");
        assert parsed.next("2024-10-{25~29}", "2024-10-30");
        assert parsed.next("2024-10-{30~31}", "2024-11-10");
    }

    @Test
    void dayIncrementListUnsorted() {
        Parsed parsed = new Parsed("0 0 0 15/10,10/10 * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-{10~14}", "2024-10-15");
        assert parsed.next("2024-10-{15~19}", "2024-10-20");
        assert parsed.next("2024-10-{20~24}", "2024-10-25");
        assert parsed.next("2024-10-{25~29}", "2024-10-30");
        assert parsed.next("2024-10-{30~31}", "2024-11-10");
    }

    @Test
    void dayList() {
        Parsed parsed = new Parsed("0 0 0 10,20,30 * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-{10~19}", "2024-10-20");
        assert parsed.next("2024-10-{20~29}", "2024-10-30");
        assert parsed.next("2024-10-{30~31}", "2024-11-10");
    }

    @Test
    void dayListUnsorted() {
        Parsed parsed = new Parsed("0 0 0 30,20,10 * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-{10~19}", "2024-10-20");
        assert parsed.next("2024-10-{20~29}", "2024-10-30");
        assert parsed.next("2024-10-{30~31}", "2024-11-10");
    }

    @Test
    void dayRange() {
        Parsed parsed = new Parsed("0 0 0 10-15 * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-10", "2024-10-11");
        assert parsed.next("2024-10-11", "2024-10-12");
        assert parsed.next("2024-10-12", "2024-10-13");
        assert parsed.next("2024-10-13", "2024-10-14");
        assert parsed.next("2024-10-14", "2024-10-15");
        assert parsed.next("2024-10-{15~31}", "2024-11-10");
    }

    @Test
    void dayRangeList() {
        Parsed parsed = new Parsed("0 0 0 10-12,15-17 * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-10", "2024-10-11");
        assert parsed.next("2024-10-11", "2024-10-12");
        assert parsed.next("2024-10-{13~14}", "2024-10-15");
        assert parsed.next("2024-10-15", "2024-10-16");
        assert parsed.next("2024-10-16", "2024-10-17");
        assert parsed.next("2024-10-{17~31}", "2024-11-10");
    }

    @Test
    void dayRangeListUnsorted() {
        Parsed parsed = new Parsed("0 0 0 15-17,10-12 * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-10", "2024-10-11");
        assert parsed.next("2024-10-11", "2024-10-12");
        assert parsed.next("2024-10-{13~14}", "2024-10-15");
        assert parsed.next("2024-10-15", "2024-10-16");
        assert parsed.next("2024-10-16", "2024-10-17");
        assert parsed.next("2024-10-{17~31}", "2024-11-10");
    }

    @Test
    void dayLast() {
        Parsed parsed = new Parsed("0 0 0 L * *");
        assert parsed.next("2024-10-{01~30}", "2024-10-31");
        assert parsed.next("2024-10-31", "2024-11-30");

        parsed = new Parsed("0 0 0 2L * *");
        assert parsed.next("2024-10-{01~28}", "2024-10-29");
        assert parsed.next("2024-10-{29~31}", "2024-11-28");

        parsed = new Parsed("0 0 0 4L * *");
        assert parsed.next("2024-10-{01~26}", "2024-10-27");
        assert parsed.next("2024-10-{27~31}", "2024-11-26");
    }

    @Test
    void dayLastAndNumber() {
        Parsed parsed = new Parsed("0 0 0 10,L * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-{10~30}", "2024-10-31");

        parsed = new Parsed("0 0 0 10,2L * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-{10~28}", "2024-10-29");
        assert parsed.next("2024-10-{29~31}", "2024-11-10");
    }

    @Test
    void dayLastAndNumberUnsorted() {
        Parsed parsed = new Parsed("0 0 0 L,10 * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-{10~30}", "2024-10-31");

        parsed = new Parsed("0 0 0 4L,10 * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-{10~26}", "2024-10-27");
        assert parsed.next("2024-10-{27~31}", "2024-11-10");
    }

    @Test
    void dayLastAndRange() {
        Parsed parsed = new Parsed("0 0 0 10-13,L * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-10", "2024-10-11");
        assert parsed.next("2024-10-11", "2024-10-12");
        assert parsed.next("2024-10-12", "2024-10-13");
        assert parsed.next("2024-10-{13~30}", "2024-10-31");

        parsed = new Parsed("0 0 0 10L,25-28 * *");
        assert parsed.next("2024-10-{01~20}", "2024-10-21");
        assert parsed.next("2024-10-{21~24}", "2024-10-25");
        assert parsed.next("2024-10-25", "2024-10-26");
        assert parsed.next("2024-10-26", "2024-10-27");
        assert parsed.next("2024-10-27", "2024-10-28");
        assert parsed.next("2024-10-{28~31}", "2024-11-20");
    }

    @Test
    void dayLastAndRangeUnsorted() {
        Parsed parsed = new Parsed("0 0 0 L,10-13 * *");
        assert parsed.next("2024-10-{01~09}", "2024-10-10");
        assert parsed.next("2024-10-10", "2024-10-11");
        assert parsed.next("2024-10-11", "2024-10-12");
        assert parsed.next("2024-10-12", "2024-10-13");
        assert parsed.next("2024-10-{13~30}", "2024-10-31");

        parsed = new Parsed("0 0 0 25-28,10L * *");
        assert parsed.next("2024-10-{01~20}", "2024-10-21");
        assert parsed.next("2024-10-{21~24}", "2024-10-25");
        assert parsed.next("2024-10-25", "2024-10-26");
        assert parsed.next("2024-10-26", "2024-10-27");
        assert parsed.next("2024-10-27", "2024-10-28");
        assert parsed.next("2024-10-{28~31}", "2024-11-20");
    }

    @Test
    void dayWeekday() {
        Parsed parsed = new Parsed("0 0 15W * *");
        assert parsed.next("2024-10-{1~14}", "2024-10-15");
        assert parsed.next("2024-10-{15~31}", "2024-11-15");
    }

    @Test
    void dayWeekdayAndNumber() {
        Parsed parsed = new Parsed("0 0 15W,18 * *");
        assert parsed.next("2024-10-{1~14}", "2024-10-15");
        assert parsed.next("2024-10-{15~17}", "2024-10-18");
        assert parsed.next("2024-10-{18~31}", "2024-11-15");
    }

    @Test
    void dayWeekdayAndNumberUnsorted() {
        Parsed parsed = new Parsed("0 0 18,15W * *");
        assert parsed.next("2024-10-{1~14}", "2024-10-15");
        assert parsed.next("2024-10-{15~17}", "2024-10-18");
        assert parsed.next("2024-10-{18~31}", "2024-11-15");
    }

    @Test
    void dayFirstWeekday() {
        Parsed parsed = new Parsed("0 0 1W * *");
        // 2025-02-01 is SUT, but don't back to January
        assert parsed.next("2025-01-{01~31}", "2025-02-03");
        assert parsed.next("2025-02-{01~02}", "2025-02-03");
        // 2025-03-01 is SUT, but don't back to Feburary
        assert parsed.next("2025-02-{03~28}", "2025-03-03");
        // 2025-06-01 is SUN
        assert parsed.next("2025-05-{01~31}", "2025-06-02");
        assert parsed.next("2025-06-01", "2025-06-02");
        // 2025-07-01 is weekday
        assert parsed.next("2025-06-{02~30}", "2025-07-01");
    }

    @Test
    void dayLastWeekday() {
        Parsed parsed = new Parsed("0 0 LW * *");
        // 2025-01-31 is weekday
        assert parsed.next("2025-01-{01~30}", "2025-01-31");
        // 2025-05-31 is SAT
        assert parsed.next("2025-05-{01~29}", "2025-05-30");
        assert parsed.next("2025-05-{30~31}", "2025-06-30");
        // 2025-08-31 is SUN
        assert parsed.next("2025-08-{01~28}", "2025-08-29");
        assert parsed.next("2025-08-{29~31}", "2025-09-30");
    }

    @Test
    void dayInvalid() {
        assert invalidFormat("* * -1 * *");
        assert invalidFormat("* * 0 * *");
        assert invalidFormat("* * 32 * *");
        assert invalidFormat("* * 100 * *");
        assert invalidFormat("* * 0.2 * *");
        assert invalidFormat("* * # * *");
        assert invalidFormat("* * @ * *");
        assert invalidFormat("* * X * *");

        assert invalidFormat("* * 42-63 * *");
        assert invalidFormat("* * 42#3 * *");
        assert invalidFormat("0 0 0 9X * *");
    }

    @Test
    void monthName() {
        Parsed parsed = new Parsed("0 0 0 * JAN *");
        assert parsed.next("2024-10-10", "2025-01-01");

        parsed = new Parsed("0 0 0 * FEB *");
        assert parsed.next("2024-10-10", "2025-02-01");

        parsed = new Parsed("0 0 0 * MAR *");
        assert parsed.next("2024-10-10", "2025-03-01");

        parsed = new Parsed("0 0 0 * APR *");
        assert parsed.next("2024-10-10", "2025-04-01");

        parsed = new Parsed("0 0 0 * MAY *");
        assert parsed.next("2024-10-10", "2025-05-01");

        parsed = new Parsed("0 0 0 * JUN *");
        assert parsed.next("2024-10-10", "2025-06-01");

        parsed = new Parsed("0 0 0 * JUL *");
        assert parsed.next("2024-10-10", "2025-07-01");

        parsed = new Parsed("0 0 0 * AUG *");
        assert parsed.next("2024-10-10", "2025-08-01");

        parsed = new Parsed("0 0 0 * SEP *");
        assert parsed.next("2024-10-10", "2025-09-01");

        parsed = new Parsed("0 0 0 * OCT *");
        assert parsed.next("2024-10-10", "2024-10-11");

        parsed = new Parsed("0 0 0 * NOV *");
        assert parsed.next("2024-10-10", "2024-11-01");

        parsed = new Parsed("0 0 0 * DEC *");
        assert parsed.next("2024-10-10", "2024-12-01");
    }

    @Test
    void monthNameList() {
        Parsed parsed = new Parsed("0 0 0 5 MAR,JUN,SEP *");
        assert parsed.next("2024-{01~02}-05", "2024-03-05");
        assert parsed.next("2024-{03~05}-05", "2024-06-05");
        assert parsed.next("2024-{06~08}-05", "2024-09-05");
        assert parsed.next("2024-{09~12}-05", "2025-03-05");
    }

    @Test
    void monthNameListUnsorted() {
        Parsed parsed = new Parsed("0 0 0 5 JUN,SEP,MAR *");
        assert parsed.next("2024-{01~02}-05", "2024-03-05");
        assert parsed.next("2024-{03~05}-05", "2024-06-05");
        assert parsed.next("2024-{06~08}-05", "2024-09-05");
        assert parsed.next("2024-{09~12}-05", "2025-03-05");
    }

    @Test
    void monthNumber() {
        Parsed parsed = new Parsed("0 0 0 1 11 *");
        assert parsed.next("2024-10-01", "2024-11-01");
        assert parsed.next("2024-11-01", "2025-11-01");
        assert parsed.next("2024-12-01", "2025-11-01");
        assert parsed.next("2025-01-01", "2025-11-01");
    }

    @Test
    void monthList() {
        Parsed parsed = new Parsed("0 0 0 5 3,6,9 *");
        assert parsed.next("2024-{01~02}-05", "2024-03-05");
        assert parsed.next("2024-{03~05}-05", "2024-06-05");
        assert parsed.next("2024-{06~08}-05", "2024-09-05");
        assert parsed.next("2024-{09~12}-05", "2025-03-05");
    }

    @Test
    void monthListUnsorted() {
        Parsed parsed = new Parsed("0 0 0 5 9,3,6 *");
        assert parsed.next("2024-{01~02}-05", "2024-03-05");
        assert parsed.next("2024-{03~05}-05", "2024-06-05");
        assert parsed.next("2024-{06~08}-05", "2024-09-05");
        assert parsed.next("2024-{09~12}-05", "2025-03-05");
    }

    @Test
    void monthIncrement() {
        Parsed parsed = new Parsed("0 0 0 5 5/5 *");
        assert parsed.next("2024-{01~04}-05", "2024-05-05");
        assert parsed.next("2024-{05~09}-05", "2024-10-05");
        assert parsed.next("2024-{10~12}-05", "2025-05-05");
        assert parsed.next("2025-{01~04}-05", "2025-05-05");
    }

    @Test
    void monthIncrementList() {
        Parsed parsed = new Parsed("0 0 0 5 3/5,5/5 *");
        assert parsed.next("2024-{01~02}-05", "2024-03-05");
        assert parsed.next("2024-{03~04}-05", "2024-05-05");
        assert parsed.next("2024-{05~07}-05", "2024-08-05");
        assert parsed.next("2024-{08~09}-05", "2024-10-05");
        assert parsed.next("2024-{10~12}-05", "2025-03-05");
        assert parsed.next("2025-{01~02}-05", "2025-03-05");
    }

    @Test
    void monthIncrementListUnsorted() {
        Parsed parsed = new Parsed("0 0 0 5 5/5,3/5 *");
        assert parsed.next("2024-{01~02}-05", "2024-03-05");
        assert parsed.next("2024-{03~04}-05", "2024-05-05");
        assert parsed.next("2024-{05~07}-05", "2024-08-05");
        assert parsed.next("2024-{08~09}-05", "2024-10-05");
        assert parsed.next("2024-{10~12}-05", "2025-03-05");
        assert parsed.next("2025-{01~02}-05", "2025-03-05");
    }

    @Test
    void monthNameIncrement() {
        Parsed parsed = new Parsed("0 0 0 5 MAY/5 *");
        assert parsed.next("2024-{01~04}-05", "2024-05-05");
        assert parsed.next("2024-{05~09}-05", "2024-10-05");
        assert parsed.next("2024-{10~12}-05", "2025-05-05");
        assert parsed.next("2025-{01~04}-05", "2025-05-05");
    }

    @Test
    void monthNameIncrementList() {
        Parsed parsed = new Parsed("0 0 0 5 MAR/5,MAY/5 *");
        assert parsed.next("2024-{01~02}-05", "2024-03-05");
        assert parsed.next("2024-{03~04}-05", "2024-05-05");
        assert parsed.next("2024-{05~07}-05", "2024-08-05");
        assert parsed.next("2024-{08~09}-05", "2024-10-05");
        assert parsed.next("2024-{10~12}-05", "2025-03-05");
        assert parsed.next("2025-{01~02}-05", "2025-03-05");
    }

    @Test
    void monthNameIncrementListUnsorted() {
        Parsed parsed = new Parsed("0 0 0 5 MAY/5,MAR/5 *");
        assert parsed.next("2024-{01~02}-05", "2024-03-05");
        assert parsed.next("2024-{03~04}-05", "2024-05-05");
        assert parsed.next("2024-{05~07}-05", "2024-08-05");
        assert parsed.next("2024-{08~09}-05", "2024-10-05");
        assert parsed.next("2024-{10~12}-05", "2025-03-05");
        assert parsed.next("2025-{01~02}-05", "2025-03-05");
    }

    @Test
    void monthInvalid() {
        assert invalidFormat("* * * -1 *");
        assert invalidFormat("* * * 0 *");
        assert invalidFormat("* * * 13 *");
        assert invalidFormat("* * * 100 *");
        assert invalidFormat("* * * 0.2 *");
        assert invalidFormat("* * * # *");
        assert invalidFormat("* * * @ *");
        assert invalidFormat("* * * X *");

        assert invalidFormat("* * * 42-63 *");
        assert invalidFormat("* * * 42#3 *");
        assert invalidFormat("0 0 0 1 ? *");
    }

    @Test
    void dowName() {
        Parsed parsed = new Parsed("0 0 0 * * SUN");
        assert parsed.next("2024-10-10", "2024-10-13");

        parsed = new Parsed("0 0 0 * * MON");
        assert parsed.next("2024-10-10", "2024-10-14");

        parsed = new Parsed("0 0 0 * * TUE");
        assert parsed.next("2024-10-10", "2024-10-15");

        parsed = new Parsed("0 0 0 * * WED");
        assert parsed.next("2024-10-10", "2024-10-16");

        parsed = new Parsed("0 0 0 * * THU");
        assert parsed.next("2024-10-10", "2024-10-17");

        parsed = new Parsed("0 0 0 * * FRI");
        assert parsed.next("2024-10-10", "2024-10-11");

        parsed = new Parsed("0 0 0 * * SAT");
        assert parsed.next("2024-10-10", "2024-10-12");
    }

    @Test
    void dowNumber() {
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

        parsed = new Parsed("0 0 0 * * 7");
        assert parsed.next("2024-10-{10~12}", "2024-10-13");
        assert parsed.next("2024-10-{13~19}", "2024-10-20");
        assert parsed.next("2024-10-{20~26}", "2024-10-27");
        assert parsed.next("2024-10-{27~31}", "2024-11-03");
        assert parsed.next("2024-11-{01~02}", "2024-11-03");
    }

    @Test
    void dowNumberZero() {
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
    void dowIncrement() {
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
    void dowStartIncrement() {
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
    void dowListNum() {
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
    void dowListNumUnsorted() {
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
    void dowListName() {
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
    void dowListNameUnsorted() {
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
    void dowLastFridayInMonth() {
        Parsed parsed = new Parsed("0 0 0 * * 5L");
        assert parsed.next("2024-07-{01~25}", "2024-07-26");
        assert parsed.next("2024-07-{26~30}", "2024-08-30");
        assert parsed.next("2024-08-{01~29}", "2024-08-30");
        assert parsed.next("2024-09-{01~26}", "2024-09-27");
    }

    @Test
    void dowLastFridayInMonthByName() {
        Parsed parsed = new Parsed("0 0 0 * * FRIL");
        assert parsed.next("2024-07-{01~25}", "2024-07-26");
        assert parsed.next("2024-07-{26~30}", "2024-08-30");
        assert parsed.next("2024-08-{01~29}", "2024-08-30");
        assert parsed.next("2024-09-{01~26}", "2024-09-27");
    }

    @Test
    void dowNth() {
        Parsed parsed = new Parsed("0 0 0 * * 5#3");
        assert parsed.next("2024-07-{01~18}", "2024-07-19");
        assert parsed.next("2024-07-{19~31}", "2024-08-16");
        assert parsed.next("2024-08-{01~15}", "2024-08-16");
        assert parsed.next("2024-08-{16~31}", "2024-09-20");

        parsed = new Parsed("0 0 0 * * 1#5");
        assert parsed.next("2024-07-{01~28}", "2024-07-29");
        assert parsed.next("2024-07-{29~31}", "2024-09-30");
        assert parsed.next("2024-08-{01~31}", "2024-09-30");
        assert parsed.next("2024-09-{01~29}", "2024-09-30");
        assert parsed.next("2024-10-{01~31}", "2024-12-30");
    }

    @Test
    void dowNthList() {
        Parsed parsed = new Parsed("0 0 0 * * 5#3,4#1,1#2");
        assert parsed.next("2024-07-{01~03}", "2024-07-04");
        assert parsed.next("2024-07-{04~07}", "2024-07-08");
        assert parsed.next("2024-07-{08~18}", "2024-07-19");
        assert parsed.next("2024-07-{19~31}", "2024-08-01");
    }

    @Test
    void dowNthByName() {
        Parsed parsed = new Parsed("0 0 0 * * FRI#3");
        assert parsed.next("2024-07-{01~18}", "2024-07-19");
        assert parsed.next("2024-07-{19~31}", "2024-08-16");
        assert parsed.next("2024-08-{01~15}", "2024-08-16");
        assert parsed.next("2024-08-{16~31}", "2024-09-20");

        parsed = new Parsed("0 0 0 * * MON#5");
        assert parsed.next("2024-07-{01~28}", "2024-07-29");
        assert parsed.next("2024-07-{29~31}", "2024-09-30");
        assert parsed.next("2024-08-{01~31}", "2024-09-30");
        assert parsed.next("2024-09-{01~29}", "2024-09-30");
        assert parsed.next("2024-10-{01~31}", "2024-12-30");
    }

    @Test
    void dowInvalid() {
        assert invalidFormat("* * * * -1");
        assert invalidFormat("* * * * 8");
        assert invalidFormat("* * * * 100");
        assert invalidFormat("* * * * 0.2");
        assert invalidFormat("* * * * #");
        assert invalidFormat("* * * * @");
        assert invalidFormat("* * * * X");

        assert invalidFormat("0 0 0 * * 5W");
        assert invalidFormat("0 0 0 * * 5?3");
        assert invalidFormat("0 0 0 * * 5*3");
        assert invalidFormat("0 0 0 * * 12");
    }

    @Test
    void notSupportRollingPeriod() {
        assert invalidFormat("* * 5-1 * * *");
    }

    @Test
    void withoutSeconds() {
        ZonedDateTime after = ZonedDateTime.of(2012, 3, 1, 0, 0, 0, 0, zoneId);
        ZonedDateTime expected = ZonedDateTime.of(2016, 2, 29, 0, 0, 0, 0, zoneId);
        assert new Parsed("* * 29 2 *").next(after).equals(expected);
    }

    @Test
    void triggerProblemSameMonth() {
        assertEquals(ZonedDateTime.parse("2020-01-02T00:50:00Z"), new Parsed("00 50 * 1-8 1 *")
                .next(ZonedDateTime.parse("2020-01-01T23:50:00Z")));
    }

    @Test
    void triggerProblemNextMonth() {
        assertEquals(ZonedDateTime.parse("2020-02-01T00:50:00Z"), new Parsed("00 50 * 1-8 2 *")
                .next(ZonedDateTime.parse("2020-01-31T23:50:00Z")));
    }

    @Test
    void triggerProblemNextYear() {
        assertEquals(ZonedDateTime.parse("2020-01-01T00:50:00Z"), new Parsed("00 50 * 1-8 1 *")
                .next(ZonedDateTime.parse("2019-12-31T23:50:00Z")));
    }

    @Test
    void triggerProblemNextMonthMonthAst() {
        assertEquals(ZonedDateTime.parse("2020-02-01T00:50:00Z"), new Parsed("00 50 * 1-8 * *")
                .next(ZonedDateTime.parse("2020-01-31T23:50:00Z")));
    }

    @Test
    void triggerProblemNextYearMonthAst() {
        assertEquals(ZonedDateTime.parse("2020-01-01T00:50:00Z"), new Parsed("00 50 * 1-8 * *")
                .next(ZonedDateTime.parse("2019-12-31T23:50:00Z")));
    }

    @Test
    void triggerProblemNextMonthDayAst() {
        assertEquals(ZonedDateTime.parse("2020-02-01T00:50:00Z"), new Parsed("00 50 * * 2 *")
                .next(ZonedDateTime.parse("2020-01-31T23:50:00Z")));
    }

    @Test
    void triggerProblemNextYearDayAst() {
        assertEquals(ZonedDateTime.parse("2020-01-01T00:50:00Z"), new Parsed("00 50 * * 1 *")
                .next(ZonedDateTime.parse("2019-12-31T22:50:00Z")));
    }

    @Test
    void triggerProblemNextMonthAllAst() {
        assertEquals(ZonedDateTime.parse("2020-02-01T00:50:00Z"), new Parsed("00 50 * * * *")
                .next(ZonedDateTime.parse("2020-01-31T23:50:00Z")));
    }

    @Test
    void triggerProblemNextYearAllAst() {
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
            ZonedDateTime nextDate = parse(expectedNext);

            if (base.indexOf("{") == -1) {
                ZonedDateTime baseDate = parse(base);
                assert next(baseDate).isEqual(nextDate) : base + "   " + nextDate;
                return true;
            } else {
                for (String expand : expandDateRange(base)) {
                    ZonedDateTime baseDate = parse(expand);
                    assert next(baseDate).isEqual(nextDate) : base + "   " + nextDate;
                }
            }
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

        /**
         * Expands the given date range string into all possible date combinations
         * by expanding ranges specified in the format {start~end}.
         * 
         * Example: "2024-{10~12}-{1~5}" will produce dates like "2024-10-01",
         * "2024-10-02", ..., "2024-12-05".
         * 
         * @param input The date range string to expand.
         * @return A list of strings containing all possible date combinations.
         */
        private List<String> expandDateRange(String input) {
            // Extract and expand range parts inside {} using regular expressions
            List<String> expandedList = new ArrayList<>();
            expand(input, expandedList);

            return expandedList;
        }

        /**
         * Recursively expands the date range by finding and processing the first
         * range enclosed in curly braces `{}`.
         * 
         * For each range, it replaces the placeholder with all values from the start
         * to the end of the range and calls itself to handle the remaining parts of
         * the string, if any.
         * 
         * @param input The string containing date ranges in `{start~end}` format.
         * @param results A list that will be populated with the expanded date strings.
         */
        private void expand(String input, List<String> results) {
            int openBrace = input.indexOf('{');
            int closeBrace = input.indexOf('}');

            // If no more {} is found, or all ranges have been expanded
            if (openBrace == -1 || closeBrace == -1 || openBrace > closeBrace) {
                results.add(input);
                return;
            }

            // Get the range inside {} and split it to extract the numeric values
            String prefix = input.substring(0, openBrace);
            String suffix = input.substring(closeBrace + 1);
            String rangePart = input.substring(openBrace + 1, closeBrace);
            String[] range = rangePart.split("~");
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);

            // Generate values from start to end and recursively expand them
            for (int i = start; i <= end; i++) {
                // Format to two digits
                String formatted = String.format("%02d", i);

                // Recursively process the next {}
                expand(prefix + formatted + suffix, results);
            }
        }
    }
}