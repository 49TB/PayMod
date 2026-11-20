package com.paymentfx;

import com.paymentfx.config.MissingClothScreen;
import com.paymentfx.config.PaymentFxConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public final class PaymentFxModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return MissingClothScreen::new;
        }
        return PaymentFxConfigScreen::create;
    }
}
