package com.quasar.ledger.common.util;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.exception.ApiException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * An inclusive UTC date range from query parameters {@code from} / {@code to} (yyyy-MM-dd).
 * {@code fromInclusive} / {@code toExclusive} are timestamp strings comparable with stored values.
 */
public record DateRange(String fromInclusive, String toExclusive) {

    public static final DateRange ALL = new DateRange(null, null);

    public static DateRange parse(String from, String to) {
        LocalDate f = parseDate("from", from);
        LocalDate t = parseDate("to", to);
        if (f != null && t != null && t.isBefore(f)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "'to' must not be before 'from'");
        }
        return new DateRange(f == null ? null : startOf(f), t == null ? null : startOf(t.plusDays(1)));
    }

    /** Range with a default window of {@code defaultDays} days ending today (FR-PAY-07). */
    public static DateRange parseWithDefault(String from, String to, int defaultDays, Clock clock) {
        if (from == null && to == null) {
            LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
            return new DateRange(startOf(today.minusDays(defaultDays - 1L)), startOf(today.plusDays(1)));
        }
        return parse(from, to);
    }

    private static LocalDate parseDate(String name, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "'" + name + "' must be a date in yyyy-MM-dd format");
        }
    }

    private static String startOf(LocalDate date) {
        return Timestamps.format(date.atStartOfDay().toInstant(ZoneOffset.UTC));
    }
}
