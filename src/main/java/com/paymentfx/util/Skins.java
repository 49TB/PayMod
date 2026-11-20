package com.paymentfx.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Resolves a player skin texture from a name, falling back to the default skin. */
public final class Skins {
    /** Bounded, self-expiring cache: the HUD asks for a texture every frame while a toast shows. */
    private static final long CACHE_MS = 4_000L;
    private static final int CACHE_LIMIT = 64;
    private static final Map<String, Entry> CACHE = new LinkedHashMap<>();

    private Skins() {
    }

    private record Entry(Identifier texture, long atMs) {
    }

    public static Identifier texture(String name) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            for (AbstractClientPlayer player : client.level.players()) {
                if (player.getGameProfile().name().equalsIgnoreCase(name)) {
                    return player.getSkin().body().texturePath();
                }
            }
        }
        String key = name.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        Entry cached = CACHE.get(key);
        if (cached != null && now - cached.atMs() < CACHE_MS) {
            return cached.texture();
        }
        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)), name);
        PlayerSkin skin = client.getSkinManager().createLookup(profile, false).get();
        Identifier texture = skin.body().texturePath();
        prune(now);
        CACHE.put(key, new Entry(texture, now));
        return texture;
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
