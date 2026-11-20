package com.paymentfx.command;

import com.paymentfx.config.PaymentFxConfigScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;

/** `/paymentfx config` opens the built-in settings screen, exactly like the Mod Menu button. */
public final class PaymentFxCommand {
    private PaymentFxCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("paymentfx")
                        .executes(context -> open())
                        .then(ClientCommandManager.literal("config").executes(context -> open()))));
    }

    private static int open() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(PaymentFxConfigScreen.create(client.screen)));
        return 1;
    }
}
