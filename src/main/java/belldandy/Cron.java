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

import static java.time.temporal.ChronoUnit.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
 * Represents a cron expression and provides methods to calculate the next execution time.
 * This class supports both 5-field (minute, hour, day of month, month, day of week)
 * and 6-field (second, minute, hour, day of month, month, day of week) cron expressions.
 */
class Cron {

    /** Field representing seconds in the cron expression. */
    private final Field[] fields;

    /**
     * Constructs a new Cron instance based on the given cron expression.
     *
     * @param expr The cron expression string.
     * @throws IllegalArgumentException if the expression is invalid or has an incorrect number of
     *             fields.
     */
    Cron(String expr) {
        String[] parts = expr.split("\\s+");
        int i = switch (parts.length) {
        case 5 -> 0;
        case 6 -> 1;
        default -> throw new IllegalArgumentException(expr);
        };

        fields = new Field[] { //
                new Field(Type.SECOND, i == 1 ? parts[0] : "0"), new Field(Type.MINUTE, parts[i++]), new Field(Type.HOUR, parts[i++]),
                new Field(Type.DAY_OF_MONTH, parts[i++]), new Field(Type.MONTH, parts[i++]), new Field(Type.DAY_OF_WEEK, parts[i++])};
    }

    /**
     * Calculates the next execution time after the given base time.
     * This method searches for the next execution time within the next four years.
     *
     * @param base The base time to start the search from.
     * @return The next execution time as a ZonedDateTime.
     * @throws IllegalArgumentException if no execution time can be found within four years.
     */
    public ZonedDateTime next(ZonedDateTime base) {
        // The range is four years, taking into account leap years.
        return next(base, base.plusYears(4));
    }

    /**
     * Calculates the next execution time after the given base time and before the given limit.
     *
     * @param base The base time to start the search from.
     * @param limit The upper limit for the search.
     * @return The next execution time as a ZonedDateTime.
     * @throws IllegalArgumentException if no execution time can be found before the limit.
     */
    public ZonedDateTime next(ZonedDateTime base, ZonedDateTime limit) {
        ZonedDateTime[] next = {base.plusSeconds(1).truncatedTo(ChronoUnit.SECONDS)};

        root: while (true) {
            if (next[0].isAfter(limit)) throw new IllegalArgumentException("Next time is not found before " + limit);
            if (!fields[4].nextMatch(next)) continue;

            int month = next[0].getMonthValue();
            while (!(fields[3].matchesDay(next[0].toLocalDate()) && fields[5].matchesDoW(next[0].toLocalDate()))) {
                next[0] = next[0].plusDays(1).truncatedTo(ChronoUnit.DAYS);
                if (next[0].getMonthValue() != month) continue root;
            }

            if (!fields[2].nextMatch(next)) continue;
            if (!fields[1].nextMatch(next)) continue;
            if (!fields[0].nextMatch(next)) continue;
            return next[0];
        }
    }

    /**
     * Build error message.
     * 
     * @param cron
     * @return
     */
    private static IllegalArgumentException error(String cron) {
        return new IllegalArgumentException("Invalid format '" + cron + "'");
    }

    /**
     * Represents a type of cron field (e.g., second, minute, hour, etc.).
     */
    static class Type {
        static final Type SECOND = new Type(ChronoField.SECOND_OF_MINUTE, MINUTES, 0, 59, null, "", "/");

        static final Type MINUTE = new Type(ChronoField.MINUTE_OF_HOUR, HOURS, 0, 59, null, "", "/");

        static final Type HOUR = new Type(ChronoField.HOUR_OF_DAY, DAYS, 0, 23, null, "", "/");

        static final Type DAY_OF_MONTH = new Type(ChronoField.DAY_OF_MONTH, MONTHS, 1, 31, null, "?LW", "/");

        static final Type MONTH = new Type(ChronoField.MONTH_OF_YEAR, YEARS, 1, 12, List
                .of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"), "", "/");

        static final Type DAY_OF_WEEK = new Type(ChronoField.DAY_OF_WEEK, null, 1, 7, List
                .of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"), "?L", "#/");

        private final ChronoField field;

        private final ChronoUnit upper;

        private final int min, max;

        private final List<String> names;

        private final int[] modifier;

        private final int[] increment;

        /**
         * Constructs a new Type instance.
         *
         * @param field The ChronoField this type represents.
         * @param upper The upper ChronoUnit for this type.
         * @param min The minimum allowed value for this type.
         * @param max The maximum allowed value for this type.
         * @param names List of string names for this type (e.g., month names).
         * @param modifier Allowed modifiers for this type.
         * @param increment Allowed increment modifiers for this type.
         */
        private Type(ChronoField field, ChronoUnit upper, int min, int max, List<String> names, String modifier, String increment) {
            this.field = field;
            this.upper = upper;
            this.min = min;
            this.max = max;
            this.names = names;
            this.modifier = modifier.chars().toArray();
            this.increment = increment.chars().toArray();
        }

        /**
         * Maps a string representation to its corresponding numeric value for this field.
         *
         * @param name The string representation to map.
         * @return The corresponding numeric value.
         */
        private int map(String name) {
            if (names != null) {
                int index = names.indexOf(name.toUpperCase());
                if (index != -1) {
                    return index + min;
                }
            }
            int value = Integer.parseInt(name);
            return value == 0 && field == ChronoField.DAY_OF_WEEK ? 7 : value;
        }
    }

    /**
     * Represents a single field in a cron expression.
     */
    static class Field {
        private static final Pattern FORMAT = Pattern
                .compile("(?:(?:(\\*)|(\\?|L)) | ([0-9]{1,2}|[a-z]{3,3})(?:(L|W) | -([0-9]{1,2}|[a-z]{3,3}))?)(?:(/|\\#)([0-9]{1,7}))?", Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);

        private final Type type;

        final List<int[]> parts = new ArrayList();

        /**
         * Constructs a new Field instance based on the given type and expression.
         *
         * @param type The Type of this field.
         * @param expr The expression string for this field.
         * @throws IllegalArgumentException if the expression is invalid.
         */
        Field(Type type, String expr) {
            this.type = type;

            for (String range : expr.split(",")) {
                Matcher m = FORMAT.matcher(range);
                if (!m.matches()) throw error(range);

                String start = m.group(3);
                String mod = m.group(4);
                String end = m.group(5);
                String incmod = m.group(6);
                String inc = m.group(7);

                int[] part = {-1, -1, -1, 0, 0};
                if (start != null) {
                    part[0] = type.map(start);
                    part[3] = mod == null ? 0 : mod.charAt(0);
                    if (end != null) {
                        part[1] = type.map(end);
                        part[2] = 1;
                    } else if (inc != null) {
                        part[1] = type.max;
                    } else {
                        part[1] = part[0];
                    }
                } else if (m.group(1) != null) {
                    part[0] = type.min;
                    part[1] = type.max;
                    part[2] = 1;
                } else if (m.group(2) != null) {
                    part[3] = m.group(2).charAt(0);
                } else {
                    throw error(range);
                }

                if (inc != null) {
                    part[4] = incmod.charAt(0);
                    part[2] = Integer.parseInt(inc);
                }

                // validate range
                if ((part[0] != -1 && part[0] < type.min) || (part[1] != -1 && part[1] > type.max) || (part[0] != -1 && part[1] != -1 && part[0] > part[1])) {
                    throw error(range);
                }

                // validate part
                if (part[3] != 0 && Arrays.binarySearch(type.modifier, part[3]) < 0) {
                    throw error(String.valueOf((char) part[3]));
                } else if (part[4] != 0 && Arrays.binarySearch(type.increment, part[4]) < 0) {
                    throw error(String.valueOf((char) part[4]));
                }
                parts.add(part);
            }

            Collections.sort(parts, (x, y) -> Integer.compare(x[0], y[0]));
        }

        /**
         * Checks if the given value matches the given Part.
         *
         * @param value The value to check.
         * @param part The Part to match against.
         * @return true if the value matches, false otherwise.
         */
        boolean matches(int value, int[] part) {
            return part[3] == '?' || (part[0] <= value && value <= part[1] && (value - part[0]) % part[2] == 0);
        }

        /**
         * Checks if the given date matches this field's day of month constraints.
         *
         * @param date The LocalDate to check.
         * @return true if the date matches, false otherwise.
         */
        boolean matchesDay(LocalDate date) {
            for (int[] part : parts) {
                if (part[3] == 'L') {
                    YearMonth ym = YearMonth.of(date.getYear(), date.getMonth().getValue());
                    return date.getDayOfMonth() == (ym.lengthOfMonth() - (part[0] == -1 ? 0 : part[0]));
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
                } else if (matches(date.getDayOfMonth(), part)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks if the given date matches this field's day of week constraints.
         *
         * @param date The LocalDate to check.
         * @return true if the date matches, false otherwise.
         */
        boolean matchesDoW(LocalDate date) {
            for (int[] part : parts) {
                if (part[3] == 'L') {
                    YearMonth ym = YearMonth.of(date.getYear(), date.getMonth().getValue());
                    return date.getDayOfWeek() == DayOfWeek.of(part[0]) && date.getDayOfMonth() > (ym.lengthOfMonth() - 7);
                } else if (part[4] == '#') {
                    if (date.getDayOfWeek() == DayOfWeek.of(part[0])) {
                        int num = date.getDayOfMonth() / 7;
                        return part[2] == (date.getDayOfMonth() % 7 == 0 ? num : num + 1);
                    }
                    return false;
                } else if (matches(date.getDayOfWeek().getValue(), part)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Finds the next matching value for this field.
         *
         * @param dateTime Array containing a single ZonedDateTime to be updated.
         * @return true if a match was found, false if the field overflowed.
         */
        private boolean nextMatch(ZonedDateTime[] dateTime) {
            int value = dateTime[0].get(type.field);

            for (int[] part : parts) {
                int nextMatch = nextMatch(value, part);
                if (nextMatch > -1) {
                    if (nextMatch != value) {
                        dateTime[0] = switch (type.field) {
                        case DAY_OF_WEEK -> throw new UnsupportedOperationException();
                        case MONTH_OF_YEAR -> dateTime[0].withMonth(nextMatch).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                        default -> dateTime[0].with(type.field, nextMatch).truncatedTo(type.field.getBaseUnit());
                        };
                    }
                    return true;
                }
            }

            dateTime[0] = switch (type.field) {
            case DAY_OF_WEEK -> throw new UnsupportedOperationException();
            case MONTH_OF_YEAR -> dateTime[0].plusYears(1).withMonth(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            default -> dateTime[0].plus(1, type.upper).with(type.field, type.min).truncatedTo(type.field.getBaseUnit());
            };
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
    }
}