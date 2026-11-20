package com.paymentfx.effect;

import com.paymentfx.config.PaymentFxConfig;

/** The five escalation tiers. Thresholds and on/off switches come from the config. */
public enum Tier {
    TOAST(1, "Toast", 4_500L),
    BANNER(2, "Fireworks", 6_400L),
    SLOTS(3, "Jackpot", 7_600L),
    NUKE(4, "Nuke", 11_000L),
    BLACK_HOLE(5, "Black hole", 12_000L);

    private static final Tier[] DESCENDING = {BLACK_HOLE, NUKE, SLOTS, BANNER, TOAST};

    public final int number;
    public final String label;
    public final long durationMs;

    Tier(int number, String label, long durationMs) {
        this.number = number;
        this.label = label;
        this.durationMs = durationMs;
    }

    public long threshold(PaymentFxConfig config) {
        return switch (this) {
            case TOAST -> config.tier1Min;
            case BANNER -> config.tier2Min;
            case SLOTS -> config.tier3Min;
            case NUKE -> config.tier4Min;
            case BLACK_HOLE -> config.tier5Min;
        };
    }

    public boolean isEnabled(PaymentFxConfig config) {
        return switch (this) {
            case TOAST -> config.tier1Enabled;
            case BANNER -> config.tier2Enabled;
            case SLOTS -> config.tier3Enabled;
            case NUKE -> config.tier4Enabled;
            case BLACK_HOLE -> config.tier5Enabled;
        };
    }

    /**
     * Picks the highest enabled tier whose threshold the amount reaches, falling back to the next
     * enabled tier below it. Returns {@code null} when the amount matches no enabled tier, in
     * which case no animation is played at all.
     */
    public static Tier of(long amount, PaymentFxConfig config) {
        for (Tier tier : DESCENDING) {
            if (tier.isEnabled(config) && amount >= tier.threshold(config)) {
                return tier;
            }
        }
        return null;
    }
}
