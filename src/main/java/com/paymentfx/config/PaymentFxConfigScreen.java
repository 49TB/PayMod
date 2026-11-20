package com.paymentfx.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Cloth Config screen, opened from ModMenu. */
public final class PaymentFxConfigScreen {
    private PaymentFxConfigScreen() {
    }

    public static Screen create(Screen parent) {
        PaymentFxConfig config = PaymentFxConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("paymentfx.config.title"))
                .setSavingRunnable(() -> {
                    PaymentFxConfig.normalize(config);
                    PaymentFxConfig.save();
                });

        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("paymentfx.config.general"));
        general.addEntry(entries.startBooleanToggle(Component.translatable("paymentfx.config.enabled"), config.enabled)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.enabled = value)
                .build());
        general.addEntry(entries.startIntSlider(Component.translatable("paymentfx.config.volume"), Math.round(config.masterVolume * 100.0F), 0, 100)
                .setDefaultValue(100)
                .setSaveConsumer(value -> config.masterVolume = value / 100.0F)
                .build());

        ConfigCategory thresholds = builder.getOrCreateCategory(Component.translatable("paymentfx.config.thresholds"));
        thresholds.addEntry(entries.startLongField(Component.translatable("paymentfx.config.tier2"), config.tier2Min)
                .setDefaultValue(10_000_000L)
                .setTooltip(Component.translatable("paymentfx.config.thresholds.tooltip"))
                .setSaveConsumer(value -> config.tier2Min = value)
                .build());
        thresholds.addEntry(entries.startLongField(Component.translatable("paymentfx.config.tier3"), config.tier3Min)
                .setDefaultValue(500_000_000L)
                .setSaveConsumer(value -> config.tier3Min = value)
                .build());
        thresholds.addEntry(entries.startLongField(Component.translatable("paymentfx.config.tier4"), config.tier4Min)
                .setDefaultValue(1_000_000_000L)
                .setSaveConsumer(value -> config.tier4Min = value)
                .build());
        thresholds.addEntry(entries.startLongField(Component.translatable("paymentfx.config.tier5"), config.tier5Min)
                .setDefaultValue(10_000_000_000L)
                .setSaveConsumer(value -> config.tier5Min = value)
                .build());

        return builder.build();
    }
}
