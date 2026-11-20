package com.paymentfx.config;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Shown when ModMenu is installed without Cloth Config. */
public final class MissingClothScreen extends Screen {
    private final Screen parent;

    public MissingClothScreen(Screen parent) {
        super(Text.literal("PaymentFX"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(this.width / 2 - 50, this.height / 2 + 24, 100, 20)
                .build());
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Install Cloth Config API to edit PaymentFX settings."),
                this.width / 2, this.height / 2 - 10, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
