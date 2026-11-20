package com.paymentfx;

import com.paymentfx.chat.PaymentListener;
import com.paymentfx.command.PaymentFxCommand;
import com.paymentfx.command.TestPayCommand;
import com.paymentfx.config.PaymentFxConfig;
import com.paymentfx.effect.EffectManager;
import com.paymentfx.effect.WorldFx;
import com.paymentfx.hud.HudFx;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.resources.Identifier;

/** Client-only entrypoint. Nothing here touches the world data; all effects are rendered locally. */
public class PaymentFxClient implements ClientModInitializer {
    public static final String MOD_ID = "paymentfx";

    @Override
    public void onInitializeClient() {
        PaymentFxConfig.load();
        PaymentListener.register();
        TestPayCommand.register();
        PaymentFxCommand.register();

        // 1.21.9+ replaced HudRenderCallback with identified HUD elements.
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "overlay"), HudFx::render);
        WorldRenderEvents.AFTER_ENTITIES.register(WorldFx::render);
        ClientTickEvents.END_CLIENT_TICK.register(EffectManager.INSTANCE::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EffectManager.INSTANCE.reset());

        PaymentFxConfig config = PaymentFxConfig.get();
        PaymentFxConfig.LOGGER.info("PaymentFX ready: tiers at {}, {}, {}, {}, {}; chat format '{}'",
                config.tier1Min, config.tier2Min, config.tier3Min, config.tier4Min, config.tier5Min,
                config.chatPattern);
    }
}
