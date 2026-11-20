package com.paymentfx;

import com.paymentfx.config.PaymentFxConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class PaymentFxModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Always a valid, non-null Screen: the factory itself picks Cloth Config or the
        // built-in vanilla screen and swallows any failure from the optional dependency.
        return PaymentFxConfigScreen::create;
    }
}
