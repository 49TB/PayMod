package com.paymentfx.chat;

import com.paymentfx.config.PaymentFxConfig;
import com.paymentfx.util.MoneyFormat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognises the server's economy message. The line shape is user-configurable through the
 * "Chat Pay Format" setting: every literal character of the pattern is escaped, whitespace
 * becomes a run of whitespace, {@code {player}} captures the sender and {@code {amount}} captures
 * the value. Vanilla format codes are stripped before matching.
 */
public final class PaymentParser {
    private static final Pattern FORMAT_CODES = Pattern.compile("\u00A7.");

    private static String cachedSource;
    private static Compiled cachedCompiled;

    /** A compiled pattern plus the group indexes of the two placeholders. */
    private record Compiled(Pattern pattern, int playerGroup, int amountGroup) {
    }

    private PaymentParser() {
    }

    /** @return the parsed payment, or {@code null} when the message is not a payment line. */
    public static Payment parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        Compiled compiled = compiledFor(PaymentFxConfig.get().chatPattern);
        if (compiled == null) {
            return null;
        }
        String text = stripFormatCodes(raw);
        Matcher matcher = compiled.pattern().matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String sender = matcher.group(compiled.playerGroup());
        String amount = matcher.group(compiled.amountGroup());
        if (sender == null || amount == null) {
            return null;
        }
        long value = MoneyFormat.parse(amount);
        if (value <= 0L) {
            return null;
        }
        return new Payment(sender, value, MoneyFormat.dollars(value));
    }

    /** Removes the Minecraft colour/style codes so a formatted chat line still matches. */
    public static String stripFormatCodes(String text) {
        return text == null ? "" : FORMAT_CODES.matcher(text).replaceAll("");
    }

    /** True when the pattern contains both placeholders, i.e. it can produce a payment at all. */
    public static boolean isValid(String pattern) {
        return pattern != null
                && pattern.contains("{player}")
                && pattern.contains("{amount}")
                && compile(pattern) != null;
    }

    private static Compiled compiledFor(String source) {
        String pattern = source == null || source.isBlank() ? PaymentFxConfig.DEFAULT_CHAT_PATTERN : source;
        if (cachedCompiled != null && pattern.equals(cachedSource)) {
            return cachedCompiled;
        }
        Compiled compiled = compile(pattern);
        if (compiled == null) {
            compiled = compile(PaymentFxConfig.DEFAULT_CHAT_PATTERN);
        }
        if (compiled != null) {
            cachedSource = pattern;
            cachedCompiled = compiled;
        }
        return compiled;
    }

    private static Compiled compile(String source) {
        if (source == null) {
            return null;
        }
        StringBuilder regex = new StringBuilder();
        int groups = 0;
        int playerGroup = -1;
        int amountGroup = -1;
        int length = source.length();
        for (int i = 0; i < length; ) {
            if (source.startsWith("{player}", i)) {
                groups++;
                if (playerGroup < 0) {
                    playerGroup = groups;
                }
                // Optional < > wrapper so "<Name> paid you $5M" still works; the capture stays
                // exactly the username.
                regex.append("<?([A-Za-z0-9_]{1,16})>?");
                i += 8;
            } else if (source.startsWith("{amount}", i)) {
                groups++;
                if (amountGroup < 0) {
                    amountGroup = groups;
                }
                regex.append("([0-9][0-9.,]*[KMBT]?)");
                i += 8;
            } else {
                char character = source.charAt(i);
                if (Character.isWhitespace(character)) {
                    while (i < length && Character.isWhitespace(source.charAt(i))) {
                        i++;
                    }
                    regex.append("\\s+");
                } else {
                    regex.append(Pattern.quote(String.valueOf(character)));
                    i++;
                }
            }
        }
        if (playerGroup < 0 || amountGroup < 0) {
            return null;
        }
        try {
            return new Compiled(Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE), playerGroup, amountGroup);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
