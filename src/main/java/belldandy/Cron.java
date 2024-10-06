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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

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
        ZonedDateTime limit = base.plusYears(4);

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
}