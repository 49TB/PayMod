package com.paymentfx.chat;

import com.paymentfx.util.MoneyFormat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognises the economy message "&lt;PlayerName&gt; paid you $&lt;Amount&gt;".
 * Vanilla format codes are already stripped by the caller, so the match runs on plain text.
 */
public final class PaymentParser {
    private static final Pattern PAYMENT = Pattern.compile(
            "^\\s*(?:\\[[^\\]\\s]{1,32}\\]\\s*)?(?:<)?([A-Za-z0-9_]{1,16})(?:>)?\\s+paid\\s+you\\s+\\$\\s*"
                    + "([0-9][0-9,_]*(?:\\.[0-9]+)?)\\s*([KMBT])?\\s*[.!]?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private PaymentParser() {
    }

    /** @return the parsed payment, or {@code null} when the message is not a payment line. */
    public static Payment parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        Matcher matcher = PAYMENT.matcher(raw);
        if (!matcher.matches()) {
            return null;
        }
        String sender = matcher.group(1);
        String suffix = matcher.group(3) == null ? "" : matcher.group(3);
        long value = MoneyFormat.parse(matcher.group(2) + suffix);
        if (value <= 0L) {
            return null;
        }
        return new Payment(sender, value, MoneyFormat.dollars(value));
    }
}
