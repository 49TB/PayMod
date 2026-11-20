package com.paymentfx.effect;

import com.paymentfx.config.PaymentFxConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Layer-by-layer sound design. Every cue is a short, scheduled vanilla sound so nothing has to be
 * stopped manually; the whole queue is dropped when its effect ends.
 */
public final class SoundFx {
    private static final List<Cue> QUEUE = new ArrayList<>();
    private static final int MAX_QUEUED = 512;

    private SoundFx() {
    }

    private record Cue(long atMs, long owner, SoundEvent sound, float volume, float pitch) {
    }

    public static void triggerFor(ActiveEffect effect) {
        long t = effect.startMs;
        long id = effect.id;
        switch (effect.tier) {
            case TOAST -> {
                at(t, id, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.75F, 1.55F);
                at(t + 55L, id, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.6F, 1.9F);
                at(t + 140L, id, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.45F, 2.0F);
            }
            case BANNER -> {
                at(t, id, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 0.9F);
                at(t + 330L, id, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.9F, 1.0F);
                at(t + 700L, id, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8F, 1.2F);
                at(t + 900L, id, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.85F, 1.05F);
                at(t + 980L, id, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.7F, 1.3F);
                at(t + 1_300L, id, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.7F, 1.4F);
                at(t + 1_380L, id, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.65F, 0.9F);
            }
            case SLOTS -> {
                for (int i = 0; i < 26; i++) {
                    at(t + 55L * i, id, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.35F, 1.75F - i * 0.01F);
                }
                for (int i = 0; i < Timelines.SLOT_LOCKS.length; i++) {
                    long lock = Timelines.SLOT_LOCKS[i];
                    at(t + lock, id, SoundEvents.BLOCK_ANVIL_LAND, 0.85F, 0.85F + i * 0.12F);
                    at(t + lock + 20L, id, SoundEvents.BLOCK_IRON_DOOR_CLOSE, 0.5F, 1.2F + i * 0.1F);
                }
                at(t + 2_820L, id, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.0F);
                float[] arpeggio = {1.0F, 1.26F, 1.5F, 2.0F, 2.52F};
                for (int i = 0; i < arpeggio.length; i++) {
                    at(t + 2_900L + i * 110L, id, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.55F, arpeggio[i]);
                    at(t + 2_940L + i * 110L, id, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 0.4F, arpeggio[i] * 1.5F);
                }
                at(t + 3_450L, id, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.5F, 1.4F);
            }
            case NUKE -> {
                at(t, id, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0F, 0.6F);
                at(t + 200L, id, SoundEvents.ENTITY_WARDEN_ROAR, 0.55F, 0.7F);
                for (int i = 0; i < 4; i++) {
                    long when = t + 300L + i * 700L;
                    at(when, id, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.85F, 0.7F + i * 0.16F);
                    at(when + 120L, id, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.7F, 0.92F + i * 0.16F);
                    at(when + 240L, id, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.3F, 0.6F + i * 0.1F);
                }
                at(t + 600L, id, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.9F, 1.9F);
                at(t + 1_500L, id, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.85F, 1.7F);
                at(t + Timelines.NUKE_FALL_END, id, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0F, 0.55F);
                at(t + Timelines.NUKE_FALL_END + 60L, id, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.95F, 0.8F);
                at(t + Timelines.NUKE_FALL_END + 140L, id, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.7F, 0.6F);
                at(t + Timelines.NUKE_FALL_END + 300L, id, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.8F, 0.42F);
                at(t + Timelines.NUKE_FALL_END + 700L, id, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.5F, 0.35F);
            }
            case BLACK_HOLE -> {
                for (int i = 0; i < 8; i++) {
                    long when = t + i * 480L;
                    at(when, id, SoundEvents.BLOCK_CONDUIT_ACTIVATE, 0.7F, 0.5F + i * 0.13F);
                    at(when + 130L, id, SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.4F, 0.6F + i * 0.12F);
                }
                at(t + Timelines.BH_COLLAPSE_END, id, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.9F, 0.45F);
                at(t + Timelines.BH_COLLAPSE_END + 40L, id, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.9F, 0.4F);
                at(t + Timelines.BH_COLLAPSE_END + 120L, id, SoundEvents.BLOCK_END_PORTAL_SPAWN, 0.8F, 0.6F);
                at(t + Timelines.BH_COLLAPSE_END + 200L, id, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 1.6F);
                float[] chimes = {1.0F, 1.2F, 1.5F, 1.8F, 2.0F, 2.4F};
                for (int i = 0; i < chimes.length; i++) {
                    at(t + Timelines.BH_COLLAPSE_END + 320L + i * 130L, id, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.6F, chimes[i]);
                }
            }
        }
    }

    private static void at(long atMs, long owner, SoundEvent sound, float volume, float pitch) {
        if (QUEUE.size() >= MAX_QUEUED) {
            QUEUE.remove(0);
        }
        QUEUE.add(new Cue(atMs, owner, sound, volume, pitch));
    }

    public static void tick(MinecraftClient client) {
        if (QUEUE.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Cue> iterator = QUEUE.iterator();
        while (iterator.hasNext()) {
            Cue cue = iterator.next();
            if (cue.atMs() <= now) {
                iterator.remove();
                play(client, cue.sound(), cue.volume(), cue.pitch());
            }
        }
    }

    private static void play(MinecraftClient client, SoundEvent sound, float volume, float pitch) {
        float master = PaymentFxConfig.get().masterVolume;
        float effective = volume * master;
        if (effective <= 0.01F || client.getSoundManager() == null) {
            return;
        }
        float clampedPitch = Math.max(0.5F, Math.min(2.0F, pitch));
        client.getSoundManager().play(PositionedSoundInstance.master(sound, effective, clampedPitch));
    }

    public static void purge(long owner) {
        QUEUE.removeIf(cue -> cue.owner() == owner);
    }

    public static void clear() {
        QUEUE.clear();
    }
}
