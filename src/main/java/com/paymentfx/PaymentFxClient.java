package com.paymentfx;

import com.paymentfx.chat.PaymentListener;
import com.paymentfx.command.TestPayCommand;
import com.paymentfx.config.PaymentFxConfig;
import com.paymentfx.effect.EffectManager;
import com.paymentfx.effect.WorldFx;
import com.paymentfx.hud.HudFx;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/** Client-only entrypoint. Nothing here touches the world data; all effects are rendered locally. */
public class PaymentFxClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PaymentFxConfig.load();
        PaymentListener.register();
        TestPayCommand.register();

        HudRenderCallback.EVENT.register(HudFx::render);
        WorldRenderEvents.LAST.register(WorldFx::render);
        ClientTickEvents.END_CLIENT_TICK.register(EffectManager.INSTANCE::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EffectManager.INSTANCE.reset());

        PaymentFxConfig.LOGGER.info("PaymentFX ready: tiers at {}, {}, {}, {}",
                PaymentFxConfig.get().tier2Min,
                PaymentFxConfig.get().tier3Min,
                PaymentFxConfig.get().tier4Min,
                PaymentFxConfig.get().tier5Min);
    }
}
