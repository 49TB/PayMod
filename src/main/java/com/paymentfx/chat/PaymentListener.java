package com.paymentfx.chat;

import com.paymentfx.effect.EffectManager;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Watches incoming client chat. Server economy plugins send these lines as game messages,
 * so {@code GAME} is the primary source; {@code CHAT} is handled too but only for senders
 * that are actually on the server player list, which filters out hand-typed fakes.
 */
public final class PaymentListener {
    private static final Map<String, Long> RECENT = new LinkedHashMap<>(16, 0.75F, false);
    private static final long DEDUPE_MS = 400L;

    private PaymentListener() {
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                handle(message, false);
            }
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> handle(message, true));
    }

    private static void handle(Text message, boolean playerChat) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        Payment payment = PaymentParser.parse(message.getString());
        if (payment == null) {
            return;
        }
        String self = client.player.getGameProfile().getName();
        if (self != null && self.equalsIgnoreCase(payment.sender())) {
            return;
        }
        if (playerChat && !isListedPlayer(client, payment.sender())) {
            return;
        }
        if (isDuplicate(message.getString())) {
            return;
        }
        EffectManager.INSTANCE.trigger(payment.sender(), payment.value(), payment.amountText(), false);
    }

    private static boolean isListedPlayer(MinecraftClient client, String name) {
        if (client.getNetworkHandler() == null) {
            return false;
        }
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile().getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDuplicate(String message) {
        long now = System.currentTimeMillis();
        Long last = RECENT.get(message);
        if (last != null && now - last < DEDUPE_MS) {
            return true;
        }
        RECENT.put(message, now);
        if (RECENT.size() > 24) {
            while (RECENT.size() > 16) {
                String oldest = RECENT.keySet().iterator().next();
                RECENT.remove(oldest);
            }
        }
        return false;
    }

}
