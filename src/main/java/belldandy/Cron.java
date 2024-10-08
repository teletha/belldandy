/*
 * Copyright (C) 2024 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package belldandy;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a single field in a cron expression.
 */
class Cron {
    private static final Pattern FORMAT = Pattern
            .compile("(?:(?:(\\*)|(\\?|L)) | ([0-9]{1,2}|[a-z]{3,3})(?:(L|W) | -([0-9]{1,2}|[a-z]{3,3}))?)(?:(/|\\#)([0-9]{1,7}))?", Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);

    private ChronoField field;

    private int min;

    private List<String> names;

    /**
     * [0] - start
     * [1] - end
     * [2] - increment
     * [3] - modifier
     * [4] - modifierForIncrement
     */
    private List<int[]> parts = new ArrayList();

    /**
     * Constructs a new Field instance based on the given type and expression.
     *
     * @param expr The expression string for this field.
     * @throws IllegalArgumentException if the expression is invalid.
     */
    Cron(ChronoField field, int min, int max, String names, String modifier, String increment, String expr) {
        this.field = field;
        this.min = min;
        this.names = Arrays.asList(names.split("(?<=\\G...)")); // split every three letters

        for (String range : expr.split(",")) {
            Matcher m = FORMAT.matcher(range);
            if (!m.matches()) error(range);

            String start = m.group(3);
            String mod = m.group(4);
            String end = m.group(5);
            String incmod = m.group(6);
            String inc = m.group(7);

            int[] part = {-1, -1, -1, 0, 0};
            if (start != null) {
                part[0] = map(start);
                part[3] = mod == null ? 0 : mod.charAt(0);
                if (end != null) {
                    part[1] = map(end);
                    part[2] = 1;
                } else if (inc != null) {
                    part[1] = max;
                } else {
                    part[1] = part[0];
                }
            } else if (m.group(1) != null) {
                part[0] = min;
                part[1] = max;
                part[2] = 1;
            } else if (m.group(2) != null) {
                part[3] = m.group(2).charAt(0);
            } else {
                error(range);
            }

            if (inc != null) {
                part[4] = incmod.charAt(0);
                part[2] = Integer.parseInt(inc);
            }

            // validate range
            if ((part[0] != -1 && part[0] < min) || (part[1] != -1 && part[1] > max) || (part[0] != -1 && part[1] != -1 && part[0] > part[1])) {
                error(range);
            }

            // validate part
            if (part[3] != 0 && modifier.indexOf(part[3]) == -1) {
                error(String.valueOf((char) part[3]));
            }
            if (part[4] != 0 && increment.indexOf(part[4]) == -1) {
                error(String.valueOf((char) part[4]));
            }
            parts.add(part);
        }

        Collections.sort(parts, (x, y) -> Integer.compare(x[0], y[0]));
    }

    /**
     * Maps a string representation to its corresponding numeric value for this field.
     *
     * @param name The string representation to map.
     * @return The corresponding numeric value.
     */
    private int map(String name) {
        int index = names.indexOf(name.toUpperCase());
        if (index != -1) {
            return index + min;
        }
        int value = Integer.parseInt(name);
        return value == 0 && field == ChronoField.DAY_OF_WEEK ? 7 : value;
    }

    /**
     * Checks if the given date matches this field's constraints.
     *
     * @param date The LocalDate to check.
     * @return true if the date matches, false otherwise.
     */
    boolean matches(ZonedDateTime date) {
        for (int[] part : parts) {
            if (part[3] == 'L') {
                YearMonth ym = YearMonth.of(date.getYear(), date.getMonth().getValue());
                if (field == ChronoField.DAY_OF_WEEK) {
                    return date.getDayOfWeek() == DayOfWeek.of(part[0]) && date.getDayOfMonth() > (ym.lengthOfMonth() - 7);
                } else {
                    return date.getDayOfMonth() == (ym.lengthOfMonth() - (part[0] == -1 ? 0 : part[0]));
                }
            } else if (part[3] == 'W') {
                if (date.getDayOfWeek().getValue() <= 5) {
                    if (date.getDayOfMonth() == part[0]) {
                        return true;
                    } else if (date.getDayOfWeek().getValue() == 5) {
                        return date.plusDays(1).getDayOfMonth() == part[0];
                    } else if (date.getDayOfWeek().getValue() == 1) {
                        return date.minusDays(1).getDayOfMonth() == part[0];
                    }
                }
            } else if (part[4] == '#') {
                if (date.getDayOfWeek() == DayOfWeek.of(part[0])) {
                    int num = date.getDayOfMonth() / 7;
                    return part[2] == (date.getDayOfMonth() % 7 == 0 ? num : num + 1);
                }
                return false;
            } else {
                int value = date.get(field);
                if (part[3] == '?' || (part[0] <= value && value <= part[1] && (value - part[0]) % part[2] == 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds the next matching value for this field.
     *
     * @param date Array containing a single ZonedDateTime to be updated.
     * @return true if a match was found, false if the field overflowed.
     */
    boolean nextMatch(ZonedDateTime[] date) {
        int value = date[0].get(field);

        for (int[] part : parts) {
            int nextMatch = nextMatch(value, part);
            if (nextMatch > -1) {
                if (nextMatch != value) {
                    if (field == ChronoField.MONTH_OF_YEAR) {
                        date[0] = date[0].withMonth(nextMatch).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                    } else {
                        date[0] = date[0].with(field, nextMatch).truncatedTo(field.getBaseUnit());
                    }
                }
                return true;
            }
        }

        if (field == ChronoField.MONTH_OF_YEAR) {
            date[0] = date[0].plusYears(1).withMonth(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        } else {
            date[0] = date[0].plus(1, field.getRangeUnit()).with(field, min).truncatedTo(field.getBaseUnit());
        }
        return false;
    }

    /**
     * Finds the next matching value within a single Part.
     *
     * @param value The current value.
     * @param part The Part to match against.
     * @return The next matching value, or -1 if no match is found.
     */
    private int nextMatch(int value, int[] part) {
        if (value > part[1]) {
            return -1;
        }
        int nextPotential = Math.max(value, part[0]);
        if (part[2] == 1 || nextPotential == part[0]) {
            return nextPotential;
        }

        int remainder = ((nextPotential - part[0]) % part[2]);
        if (remainder != 0) {
            nextPotential += part[2] - remainder;
        }

        return nextPotential <= part[1] ? nextPotential : -1;
    }

    /**
     * Throw the invalid format error.
     * 
     * @param cron
     * @return
     */
    static int error(String cron) {
        throw new IllegalArgumentException("Invalid format '" + cron + "'");
    }
}