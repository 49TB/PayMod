package com.paymentfx.effect;

import com.paymentfx.config.PaymentFxConfig;
import com.paymentfx.util.MoneyFormat;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Owns the running animations and drives their world/sound work each client tick. */
public final class EffectManager {
    public static final EffectManager INSTANCE = new EffectManager();

    private static final int MAX_EFFECTS = 8;

    private final List<ActiveEffect> effects = new ArrayList<>();
    private long nextId = 1L;
    /** Keeps previews from the config screen running even while the mod itself is switched off. */
    private long previewUntilMs;

    private EffectManager() {
    }

    public void trigger(String sender, long value, String amountText, boolean force) {
        PaymentFxConfig config = PaymentFxConfig.get();
        if ((!config.enabled && !force) || value <= 0L || sender == null || sender.isEmpty()) {
            return;
        }
        Tier tier = Tier.of(value, config);
        if (tier == null) {
            // Every tier the amount reaches is switched off: no animation at all.
            return;
        }
        spawn(tier, sender, value, amountText);
    }

    /** Plays one tier on demand for the config screen's Test buttons, ignoring thresholds and switches. */
    public void preview(Tier tier) {
        if (tier == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        String sender = client.player != null ? client.player.getGameProfile().name() : "TestPlayer";
        long value = Math.max(1L, tier.threshold(PaymentFxConfig.get()));
        this.previewUntilMs = System.currentTimeMillis() + tier.durationMs + 500L;
        spawn(tier, sender, value, MoneyFormat.dollars(value));
    }

    private void spawn(Tier tier, String sender, long value, String amountText) {
        ActiveEffect effect = new ActiveEffect(nextId++, tier, sender, value, amountText, System.currentTimeMillis());
        if (effects.size() >= MAX_EFFECTS) {
            ActiveEffect dropped = effects.remove(0);
            SoundFx.purge(dropped.id);
        }
        effects.add(effect);
        SoundFx.triggerFor(effect);
    }

    public void tick(Minecraft client) {
        PaymentFxConfig config = PaymentFxConfig.get();
        if (!config.enabled && System.currentTimeMillis() >= this.previewUntilMs) {
            if (!effects.isEmpty()) {
                reset();
            }
            return;
        }
        SoundFx.tick(client);
        long now = System.currentTimeMillis();
        Iterator<ActiveEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            ActiveEffect effect = iterator.next();
            if (client.level != null && client.player != null) {
                WorldFx.tick(client, effect, now);
            }
            if (effect.isFinished(now)) {
                SoundFx.purge(effect.id);
                iterator.remove();
            }
        }
    }

    /** Drops every animation and any queued sound. Called on disconnect and when disabled. */
    public void reset() {
        this.previewUntilMs = 0L;
        effects.clear();
        SoundFx.clear();
        WorldFx.hasShake = false;
        WorldFx.shakeYaw = 0.0F;
        WorldFx.shakePitch = 0.0F;
    }

    public List<ActiveEffect> effects() {
        return effects;
    }

    /** Timestamp until which a config-screen preview keeps the overlay drawing while disabled. */
    public long previewUntil() {
        return this.previewUntilMs;
    }

    public ActiveEffect newest(Tier minimumExcluded) {
        for (int i = effects.size() - 1; i >= 0; i--) {
            ActiveEffect effect = effects.get(i);
            if (effect.tier != minimumExcluded) {
                return effect;
            }
        }
        return null;
    }
}
