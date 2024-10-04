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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a cron expression and provides methods to calculate the next execution time.
 * This class supports both 5-field (minute, hour, day of month, month, day of week)
 * and 6-field (second, minute, hour, day of month, month, day of week) cron expressions.
 */
public class Cron {

    /** Field representing seconds in the cron expression. */
    private final Field second;

    /** Field representing minutes in the cron expression. */
    private final Field minute;

    /** Field representing hours in the cron expression. */
    private final Field hour;

    /** Field representing days of the week in the cron expression. */
    private final Field dow;

    /** Field representing months in the cron expression. */
    private final Field month;

    /** Field representing days of the month in the cron expression. */
    private final Field day;

    /**
     * Constructs a new Cron instance based on the given cron expression.
     *
     * @param expr The cron expression string.
     * @throws IllegalArgumentException if the expression is invalid or has an incorrect number of
     *             fields.
     */
    public Cron(String expr) {
        String[] parts = expr.split("\\s+");
        boolean hasSec = switch (parts.length) {
        case 5 -> false;
        case 6 -> true;
        default -> throw new IllegalArgumentException(expr);
        };

        int i = hasSec ? 1 : 0;
        this.second = new Field(Type.SECOND, hasSec ? parts[0] : "0");
        this.minute = new Field(Type.MINUTE, parts[i++]);
        this.hour = new Field(Type.HOUR, parts[i++]);
        this.day = new Field(Type.DAY_OF_MONTH, parts[i++]);
        this.month = new Field(Type.MONTH, parts[i++]);
        this.dow = new Field(Type.DAY_OF_WEEK, parts[i++]);
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
        ZonedDateTime[] next = {base.plusSeconds(1)};

        root: while (true) {
            if (next[0].isAfter(limit)) throw new IllegalArgumentException("Next time is not found before " + limit);
            if (!month.nextMatch(next)) continue;

            int month = next[0].getMonthValue();
            while (!(day.matchesDay(next[0].toLocalDate()) && dow.matchesDoW(next[0].toLocalDate()))) {
                next[0] = next[0].plusDays(1).truncatedTo(ChronoUnit.DAYS);
                if (next[0].getMonthValue() != month) continue root;
            }

            if (!hour.nextMatch(next)) continue;
            if (!minute.nextMatch(next)) continue;
            if (!second.nextMatch(next)) continue;
            return next[0];
        }
    }

    /**
     * Represents a type of cron field (e.g., second, minute, hour, etc.).
     */
    static class Type {
        static final Type SECOND = new Type(ChronoField.SECOND_OF_MINUTE, MINUTES, 0, 59, null, "", "/");

        static final Type MINUTE = new Type(ChronoField.MINUTE_OF_HOUR, HOURS, 0, 59, null, "", "/");

        static final Type HOUR = new Type(ChronoField.HOUR_OF_DAY, DAYS, 0, 23, null, "", "/");

        static final Type DAY_OF_MONTH = new Type(ChronoField.DAY_OF_MONTH, MONTHS, 1, 31, null, "LW?", "/");

        static final Type MONTH = new Type(ChronoField.MONTH_OF_YEAR, YEARS, 1, 12, List
                .of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"), "", "/");

        static final Type DAY_OF_WEEK = new Type(ChronoField.DAY_OF_WEEK, null, 1, 7, List
                .of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"), "L?", "/#");

        private final ChronoField field;

        private final ChronoUnit upper;

        final int min, max;

        private final List<String> names;

        private final List<String> modifier;

        private final List<String> increment;

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
            this.modifier = List.of(modifier.split(""));
            this.increment = List.of(increment.split(""));
        }

        /**
         * Gets the value of this field from the given ZonedDateTime.
         *
         * @param dateTime The ZonedDateTime to extract the value from.
         * @return The value of this field in the given dateTime.
         */
        private int get(ZonedDateTime dateTime) {
            return dateTime.get(field);
        }

        /**
         * Sets the value of this field in the given ZonedDateTime.
         *
         * @param dateTime The ZonedDateTime to modify.
         * @param value The new value to set.
         * @return A new ZonedDateTime with the updated field value.
         */
        private ZonedDateTime set(ZonedDateTime dateTime, int value) {
            return switch (field) {
            case DAY_OF_WEEK -> throw new UnsupportedOperationException();
            case MONTH_OF_YEAR -> dateTime.withMonth(value).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            default -> dateTime.with(field, value).truncatedTo(field.getBaseUnit());
            };
        }

        /**
         * Handles overflow of this field by incrementing the next higher field.
         *
         * @param dateTime The ZonedDateTime to modify.
         * @return A new ZonedDateTime with the next higher field incremented.
         */
        private ZonedDateTime overflow(ZonedDateTime dateTime) {
            return switch (field) {
            case DAY_OF_WEEK -> throw new UnsupportedOperationException();
            case MONTH_OF_YEAR -> dateTime.plusYears(1).withMonth(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            default -> dateTime.plus(1, upper).with(field, min).truncatedTo(field.getBaseUnit());
            };
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
        private static final Pattern CRON_FIELD_REGEXP = Pattern
                .compile("(?:(?:(?<all>\\*)|(?<ignore>\\?)|(?<last>L)) | (?<start>[0-9]{1,2}|[a-z]{3,3})(?:(?<mod>L|W) | -(?<end>[0-9]{1,2}|[a-z]{3,3}))?)(?:(?<incmod>/|\\#)(?<inc>[0-9]{1,7}))?", Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);

        final Type type;

        final List<Part> parts = new ArrayList();

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
                Matcher m = CRON_FIELD_REGEXP.matcher(range);
                if (!m.matches()) throw error(range);

                String start = m.group("start");
                String mod = m.group("mod");
                String end = m.group("end");
                String incmod = m.group("incmod");
                String inc = m.group("inc");

                Part part = new Part();
                if (start != null) {
                    part.min = type.map(start);
                    part.modifier = mod;
                    if (end != null) {
                        part.max = type.map(end);
                        part.increment = 1;
                    } else if (inc != null) {
                        part.max = type.max;
                    } else {
                        part.max = part.min;
                    }
                } else if (m.group("all") != null) {
                    part.min = type.min;
                    part.max = type.max;
                    part.increment = 1;
                } else if (m.group("ignore") != null) {
                    part.modifier = m.group("ignore");
                } else if (m.group("last") != null) {
                    part.modifier = m.group("last");
                } else {
                    throw new IllegalArgumentException("Fix '" + range + "'");
                }

                if (inc != null) {
                    part.incModifier = incmod;
                    part.increment = Integer.parseInt(inc);
                }

                // validate range
                if ((part.min != -1 && part.min < type.min) || (part.max != -1 && part.max > type.max) || (part.min != -1 && part.max != -1 && part.min > part.max)) {
                    throw error(range);
                }

                // validate part
                if (part.modifier != null && !type.modifier.contains(part.modifier)) {
                    throw error(part.modifier);
                } else if (part.incModifier != null && !type.increment.contains(part.incModifier)) {
                    throw error(part.incModifier);
                }
                parts.add(part);
            }

            Collections.sort(parts);
        }

        /**
         * Build error message.
         * 
         * @param cron
         * @return
         */
        private IllegalArgumentException error(String cron) {
            return new IllegalArgumentException("Invalid format '" + cron + "'");
        }

        /**
         * Checks if the given value matches the given Part.
         *
         * @param value The value to check.
         * @param part The Part to match against.
         * @return true if the value matches, false otherwise.
         */
        boolean matches(int value, Part part) {
            return "?".equals(part.modifier) || (part.min <= value && value <= part.max && (value - part.min) % part.increment == 0);
        }

        /**
         * Checks if the given date matches this field's day of month constraints.
         *
         * @param date The LocalDate to check.
         * @return true if the date matches, false otherwise.
         */
        boolean matchesDay(LocalDate date) {
            for (Part part : parts) {
                if ("L".equals(part.modifier)) {
                    YearMonth ym = YearMonth.of(date.getYear(), date.getMonth().getValue());
                    return date.getDayOfMonth() == (ym.lengthOfMonth() - (part.min == -1 ? 0 : part.min));
                } else if ("W".equals(part.modifier)) {
                    if (date.getDayOfWeek().getValue() <= 5) {
                        if (date.getDayOfMonth() == part.min) {
                            return true;
                        } else if (date.getDayOfWeek().getValue() == 5) {
                            return date.plusDays(1).getDayOfMonth() == part.min;
                        } else if (date.getDayOfWeek().getValue() == 1) {
                            return date.minusDays(1).getDayOfMonth() == part.min;
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
            for (Part part : parts) {
                if ("L".equals(part.modifier)) {
                    YearMonth ym = YearMonth.of(date.getYear(), date.getMonth().getValue());
                    return date.getDayOfWeek() == DayOfWeek.of(part.min) && date.getDayOfMonth() > (ym.lengthOfMonth() - 7);
                } else if ("#".equals(part.incModifier)) {
                    if (date.getDayOfWeek() == DayOfWeek.of(part.min)) {
                        int num = date.getDayOfMonth() / 7;
                        return part.increment == (date.getDayOfMonth() % 7 == 0 ? num : num + 1);
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
            int value = type.get(dateTime[0]);

            for (Part part : parts) {
                int nextMatch = nextMatch(value, part);
                if (nextMatch > -1) {
                    if (nextMatch != value) {
                        dateTime[0] = type.set(dateTime[0], nextMatch);
                    }
                    return true;
                }
            }

            dateTime[0] = type.overflow(dateTime[0]);
            return false;
        }

        /**
         * Finds the next matching value within a single Part.
         *
         * @param value The current value.
         * @param part The Part to match against.
         * @return The next matching value, or -1 if no match is found.
         */
        private int nextMatch(int value, Part part) {
            if (value > part.max) {
                return -1;
            }
            int nextPotential = Math.max(value, part.min);
            if (part.increment == 1 || nextPotential == part.min) {
                return nextPotential;
            }

            int remainder = ((nextPotential - part.min) % part.increment);
            if (remainder != 0) {
                nextPotential += part.increment - remainder;
            }

            return nextPotential <= part.max ? nextPotential : -1;
        }
    }

    /**
     * Represents a single part of a cron field expression.
     */
    static class Part implements Comparable<Part> {
        private int min = -1;

        private int max = -1;

        private int increment = -1;

        private String modifier;

        private String incModifier;

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(Part o) {
            return Integer.compare(min, o.min);
        }
    }
}