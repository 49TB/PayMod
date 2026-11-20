package com.paymentfx.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Resolves a player skin texture from a name, falling back to the default skin. */
public final class Skins {
    private Skins() {
    }

    public static SkinTextures lookup(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                if (player.getGameProfile().getName().equalsIgnoreCase(name)) {
                    return player.getSkinTextures();
                }
            }
        }
        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)), name);
        return client.getSkinProvider().getSkinTextures(profile);
    }
}
