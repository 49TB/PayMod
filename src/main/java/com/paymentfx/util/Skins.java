package com.paymentfx.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Resolves a player skin from their profile only - never from a rendered world entity.
 * <p>
 * Order of resolution:
 * <ol>
 *     <li>the local player, if the name matches;</li>
 *     <li>the connection's player list ({@link PlayerInfo}), which keeps the real, signed
 *         {@link GameProfile} for every player on the server even when their entity is outside
 *         render distance or has not been spawned at all;</li>
 *     <li>an offline profile handed to {@link net.minecraft.client.resources.SkinManager}, which
 *         yields the deterministic Steve/Alex skin for names that are not on the server
 *         (offline players and the fake names used by {@code /testpay}).</li>
 * </ol>
 * Every step is non-blocking: the skin manager returns its default skin immediately and the
 * returned {@link Supplier} upgrades to the downloaded texture as soon as it is available.
 */
public final class Skins {
    /** Bounded, self-expiring cache: the HUD asks for a texture every frame while a toast shows. */
    private static final long CACHE_MS = 4_000L;
    private static final int CACHE_LIMIT = 64;
    private static final Map<String, Entry> CACHE = new LinkedHashMap<>();

    private Skins() {
    }

    private record Entry(Supplier<PlayerSkin> skin, long atMs) {
    }

    /** Never null: falls back to the default Steve/Alex skin. */
    public static PlayerSkin skin(String name) {
        if (name == null || name.isBlank()) {
            return DefaultPlayerSkin.getDefaultSkin();
        }
        Supplier<PlayerSkin> supplier = lookup(name);
        try {
            PlayerSkin skin = supplier.get();
            if (skin != null && skin.body() != null) {
                return skin;
            }
        } catch (RuntimeException ignored) {
            // A broken or removed profile must never take the HUD down with it.
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    /** Never null: the 64x64 skin texture to draw, default skin if nothing better is known. */
    public static Identifier texture(String name) {
        return skin(name).body().texturePath();
    }

    private static Supplier<PlayerSkin> lookup(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        Entry cached = CACHE.get(key);
        if (cached != null && now - cached.atMs() < CACHE_MS) {
            return cached.skin();
        }
        Supplier<PlayerSkin> resolved = resolve(name);
        prune(now);
        CACHE.put(key, new Entry(resolved, now));
        return resolved;
    }

    private static Supplier<PlayerSkin> resolve(String name) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.getGameProfile().name().equalsIgnoreCase(name)) {
            return client.player::getSkin;
        }

        // Tab list entry: has the real profile (and its texture property) regardless of where the
        // player currently is, so this is what makes the toast work for far-away or unloaded players.
        ClientPacketListener connection = client.getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(name);
            if (info == null) {
                info = connection.getPlayerInfoIgnoreCase(name);
            }
            if (info != null) {
                PlayerInfo entry = info;
                GameProfile profile = entry.getProfile();
                return () -> {
                    try {
                        return entry.getSkin();
                    } catch (RuntimeException exception) {
                        return DefaultPlayerSkin.get(profile);
                    }
                };
            }
        }

        // Not on the server (offline player or a /testpay fake name): deterministic default skin,
        // upgraded for free if the skin manager ever manages to resolve the profile.
        GameProfile profile = new GameProfile(offlineUuid(name), name);
        Supplier<PlayerSkin> fallback = client.getSkinManager().createLookup(profile, false);
        return () -> {
            try {
                PlayerSkin skin = fallback.get();
                return skin != null ? skin : DefaultPlayerSkin.get(profile);
            } catch (RuntimeException exception) {
                return DefaultPlayerSkin.get(profile);
            }
        };
    }

    private static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static void prune(long now) {
        Iterator<Map.Entry<String, Entry>> iterator = CACHE.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().atMs() >= CACHE_MS) {
                iterator.remove();
            }
        }
        while (CACHE.size() >= CACHE_LIMIT) {
            String oldest = CACHE.keySet().iterator().next();
            CACHE.remove(oldest);
        }
    }
}
