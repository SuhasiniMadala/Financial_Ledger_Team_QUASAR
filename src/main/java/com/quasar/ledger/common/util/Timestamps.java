package com.quasar.ledger.common.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * All timestamps are stored and returned as fixed-width ISO-8601 UTC strings
 * (e.g. 2026-11-27T10:15:30.123456Z) so that text ordering equals time ordering in SQLite.
 */
public final class Timestamps {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'").withZone(ZoneOffset.UTC);

    private Timestamps() {
    }

    public static String format(Instant instant) {
        return FORMAT.format(instant.truncatedTo(ChronoUnit.MICROS));
    }

    public static Instant parse(String text) {
        return Instant.parse(text);
    }
}
