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

import static java.time.temporal.ChronoUnit.*;

import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Represents a type of cron field (e.g., second, minute, hour, etc.).
 */
class Type {
    static final Type SECOND = new Type(ChronoField.SECOND_OF_MINUTE, MINUTES, 0, 59, null, "", "/");

    static final Type MINUTE = new Type(ChronoField.MINUTE_OF_HOUR, HOURS, 0, 59, null, "", "/");

    static final Type HOUR = new Type(ChronoField.HOUR_OF_DAY, DAYS, 0, 23, null, "", "/");

    static final Type DAY_OF_MONTH = new Type(ChronoField.DAY_OF_MONTH, MONTHS, 1, 31, null, "?LW", "/");

    static final Type MONTH = new Type(ChronoField.MONTH_OF_YEAR, YEARS, 1, 12, List
            .of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"), "", "/");

    static final Type DAY_OF_WEEK = new Type(ChronoField.DAY_OF_WEEK, null, 1, 7, List
            .of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"), "?L", "#/");

    final ChronoField field;

    final ChronoUnit upper;

    final int min;

    final int max;

    private final List<String> names;

    final int[] modifier;

    final int[] increment;

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
    int map(String name) {
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