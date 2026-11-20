package com.paymentfx.config;

import net.minecraft.client.gui.screens.Screen;

/**
 * Single entry point for the Mod Menu config button and `/paymentfx config`. It always returns the
 * built-in vanilla screen: no external config library is touched, so the button cannot crash.
 */
public final class PaymentFxConfigScreen {
    private PaymentFxConfigScreen() {
    }

    public static Screen create(Screen parent) {
        return new VanillaConfigScreen(parent);
    }
}
