package com.quasar.ledger.common.util;

/**
 * Formats integer minor units for display (UI-01, FR-WAL-06). Pure integer arithmetic:
 * money is never converted to a floating-point type (TQ-6, ADR-004).
 * INR uses the rupee sign and Indian digit grouping, e.g. 10000000 paise = "₹1,00,000.00".
 */
public final class MoneyFormatter {

    private MoneyFormatter() {
    }

    public static String format(long amountMinor, String currency) {
        boolean negative = amountMinor < 0;
        // Math.abs(Long.MIN_VALUE) overflows; balances and amounts are never that large, but stay safe.
        long abs = amountMinor == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(amountMinor);
        long major = abs / 100;
        long minor = abs % 100;
        boolean indian = "INR".equals(currency);
        String grouped = indian ? groupIndian(major) : groupWestern(major);
        String symbol = switch (currency) {
            case "INR" -> "₹";
            case "USD" -> "$";
            default -> currency + " ";
        };
        return (negative ? "-" : "") + symbol + grouped + "." + (minor < 10 ? "0" : "") + minor;
    }

    static String groupIndian(long major) {
        String digits = Long.toString(major);
        if (digits.length() <= 3) {
            return digits;
        }
        String last3 = digits.substring(digits.length() - 3);
        String rest = digits.substring(0, digits.length() - 3);
        StringBuilder sb = new StringBuilder();
        int firstGroup = rest.length() % 2;
        if (firstGroup > 0) {
            sb.append(rest, 0, firstGroup);
        }
        for (int i = firstGroup; i < rest.length(); i += 2) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(rest, i, i + 2);
        }
        return sb.append(',').append(last3).toString();
    }

    static String groupWestern(long major) {
        String digits = Long.toString(major);
        StringBuilder sb = new StringBuilder();
        int lead = digits.length() % 3;
        if (lead > 0) {
            sb.append(digits, 0, lead);
        }
        for (int i = lead; i < digits.length(); i += 3) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(digits, i, i + 3);
        }
        return sb.toString();
    }
}
