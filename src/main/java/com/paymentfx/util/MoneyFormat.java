package com.paymentfx.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses in-game currency strings ("10M", "1.5B", "$500K", "1,250,000") and renders
 * values back into a compact, readable form.
 */
public final class MoneyFormat {
    public static final long INVALID = -1L;

    private static final Pattern AMOUNT = Pattern.compile(
            "^\\$?\\s*([0-9][0-9,_]*(?:\\.[0-9]+)?)\\s*([KMBT])?$",
            Pattern.CASE_INSENSITIVE);

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q"};

    private MoneyFormat() {
    }

    /** @return the parsed value, or {@link #INVALID} when the text is not a currency amount. */
    public static long parse(String raw) {
        if (raw == null) {
            return INVALID;
        }
        Matcher matcher = AMOUNT.matcher(raw.trim());
        if (!matcher.matches()) {
            return INVALID;
        }
        String number = matcher.group(1).replace(",", "").replace("_", "");
        String suffix = matcher.group(2);
        try {
            BigDecimal value = new BigDecimal(number).multiply(BigDecimal.valueOf(multiplier(suffix)));
            if (value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE / 8L)) > 0) {
                return INVALID;
            }
            return value.setScale(0, RoundingMode.DOWN).longValue();
        } catch (NumberFormatException exception) {
            return INVALID;
        }
    }

    public static long multiplier(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return 1L;
        }
        switch (Character.toUpperCase(suffix.charAt(0))) {
            case 'K':
                return 1_000L;
            case 'M':
                return 1_000_000L;
            case 'B':
                return 1_000_000_000L;
            case 'T':
                return 1_000_000_000_000L;
            default:
                return 1L;
        }
    }

    /** 10_000_000 becomes "10M", 1_500_000 becomes "1.5M", 42 becomes "42". */
    public static String compact(long value) {
        double scaled = Math.abs((double) value);
        int suffix = 0;
        while (suffix < SUFFIXES.length - 1 && scaled >= 1000.0D) {
            scaled /= 1000.0D;
            suffix++;
        }
        String sign = value < 0 ? "-" : "";
        return sign + trimDecimal(scaled) + SUFFIXES[suffix];
    }

    /** 1250000 becomes "1,250,000". */
    public static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /** Value prefixed with a dollar sign, compacted: "$10M". */
    public static String dollars(long value) {
        return "$" + compact(value);
    }

    private static String trimDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-9D) {
            return Long.toString((long) Math.rint(value));
        }
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
