package com.paymentfx.effect;

import net.minecraft.world.phys.Vec3;

/**
 * One running payment animation. Everything is derived from {@link #startMs} plus a couple of
 * scratch fields, so finishing an effect only requires dropping the object: no entities, no
 * registered renderers and no sound handles are kept around.
 */
public final class ActiveEffect {
    public final long id;
    public final Tier tier;
    public final String sender;
    public final long value;
    public final String amountText;
    public final long startMs;
    public final long durationMs;

    /** World anchors, resolved lazily on the client tick that first needs them. */
    public Vec3 anchor;
    public Vec3 target;

    private long lastEmitMs;
    private int fired;

    public ActiveEffect(long id, Tier tier, String sender, long value, String amountText, long startMs) {
        this.id = id;
        this.tier = tier;
        this.sender = sender;
        this.value = value;
        this.amountText = amountText;
        this.startMs = startMs;
        this.durationMs = tier.durationMs;
        this.lastEmitMs = startMs;
    }

    public long elapsed(long now) {
        return Math.max(0L, now - startMs);
    }

    public float progress(long now) {
        return Math.min(1.0F, elapsed(now) / (float) durationMs);
    }

    public boolean isFinished(long now) {
        return elapsed(now) >= durationMs;
    }

    /** True exactly once per bit, used for one-shot events (bursts, impacts). */
    public boolean markOnce(int bit) {
        int mask = 1 << bit;
        if ((fired & mask) != 0) {
            return false;
        }
        fired |= mask;
        return true;
    }

    /** Throttled repeat helper for particle emission. */
    public boolean every(long now, long intervalMs) {
        if (now - lastEmitMs >= intervalMs) {
            lastEmitMs = now;
            return true;
        }
        return false;
    }
}
