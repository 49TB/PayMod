package com.paymentfx.effect;

/** Shared animation timestamps, so the HUD, world renderer and sound cues stay in sync. */
public final class Timelines {
    public static final long[] FIREWORK_BURSTS = {300L, 820L, 1_340L};
    public static final long[] SLOT_LOCKS = {1_500L, 2_050L, 2_600L};

    public static final long NUKE_FALL_END = 3_200L;
    public static final long NUKE_TEXT_FROM = 5_200L;

    public static final long BH_COLLAPSE_END = 4_000L;
    public static final long BH_TEXT_FROM = 4_600L;

    private Timelines() {
    }
}
