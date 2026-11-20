package com.paymentfx.effect;

import com.paymentfx.config.PaymentFxConfig;

/** The five escalation tiers. Thresholds are read from the config so they stay customisable. */
public enum Tier {
    TOAST(1, "Toast", 4_500L),
    BANNER(2, "Fireworks", 6_400L),
    SLOTS(3, "Jackpot", 7_600L),
    NUKE(4, "Nuke", 11_000L),
    BLACK_HOLE(5, "Black hole", 12_000L);

    public final int number;
    public final String label;
    public final long durationMs;

    Tier(int number, String label, long durationMs) {
        this.number = number;
        this.label = label;
        this.durationMs = durationMs;
    }

    public static Tier of(long amount, PaymentFxConfig config) {
        if (amount >= config.tier5Min) {
            return BLACK_HOLE;
        }
        if (amount >= config.tier4Min) {
            return NUKE;
        }
        if (amount >= config.tier3Min) {
            return SLOTS;
        }
        if (amount >= config.tier2Min) {
            return BANNER;
        }
        return TOAST;
    }
}
