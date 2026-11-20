package com.paymentfx.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shown when ModMenu is installed without Cloth Config. */
public final class MissingClothScreen extends Screen {
    private final Screen parent;

    public MissingClothScreen(Screen parent) {
        super(Component.literal("PaymentFX"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(this.width / 2 - 50, this.height / 2 + 24, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font,
                Component.literal("Install Cloth Config API to edit PaymentFX settings."),
                this.width / 2, this.height / 2 - 10, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
