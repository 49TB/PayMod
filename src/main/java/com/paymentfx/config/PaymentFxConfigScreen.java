package com.paymentfx.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/**
 * Single entry point for the Mod Menu config button. It never throws and never returns null:
 * Cloth Config is used when it is installed and builds a screen successfully, otherwise the
 * built-in vanilla screen is shown.
 */
public final class PaymentFxConfigScreen {
    private PaymentFxConfigScreen() {
    }

    public static Screen create(Screen parent) {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            try {
                Screen screen = ClothConfigScreen.create(parent);
                if (screen != null) {
                    return screen;
                }
            } catch (Throwable throwable) {
                // NoClassDefFoundError / NoSuchMethodError from a missing or incompatible Cloth
                // Config build also lands here, which is exactly what must not crash the game.
                PaymentFxConfig.LOGGER.warn("Cloth Config screen unavailable, using the built-in settings screen", throwable);
            }
        }
        return new VanillaConfigScreen(parent);
    }
}
