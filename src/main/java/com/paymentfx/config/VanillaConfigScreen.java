package com.paymentfx.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Built-in settings screen written against plain vanilla widgets only: no Cloth Config, no
 * ModMenu-only types. It is used whenever Cloth Config is missing, incompatible or throws while
 * its screen is being built, so the config button can never crash the game.
 */
public final class VanillaConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int FIELD_WIDTH = 128;
    private static final int ROW_HEIGHT = 22;
    private static final long[] DEFAULTS = {10_000_000L, 500_000_000L, 1_000_000_000L, 10_000_000_000L};
    private static final String[] TIER_KEYS = {
            "paymentfx.config.tier2",
            "paymentfx.config.tier3",
            "paymentfx.config.tier4",
            "paymentfx.config.tier5"
    };

    private final Screen parent;
    private final long[] tiers = new long[4];
    private final boolean[] valid = {true, true, true, true};
    private final EditBox[] tierFields = new EditBox[4];

    private boolean enabled;
    private float volume;
    private Button enabledButton;
    private Button doneButton;
    private VolumeSlider volumeSlider;
    private Component status = Component.empty();
    private int left;
    private int top;
    private int panelWidth;

    public VanillaConfigScreen(Screen parent) {
        super(Component.literal("PaymentFX Settings"));
        this.parent = parent;
        PaymentFxConfig config = PaymentFxConfig.get();
        this.enabled = config.enabled;
        this.volume = config.masterVolume;
        this.tiers[0] = config.tier2Min;
        this.tiers[1] = config.tier3Min;
        this.tiers[2] = config.tier4Min;
        this.tiers[3] = config.tier5Min;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(PANEL_WIDTH, Math.max(180, this.width - 20));
        this.left = (this.width - this.panelWidth) / 2;
        this.top = Math.max(8, this.height / 2 - 108);
        int fieldWidth = Math.min(FIELD_WIDTH, this.panelWidth / 2);
        int fieldX = this.left + this.panelWidth - fieldWidth;

        this.enabledButton = this.addRenderableWidget(
                Button.builder(enabledLabel(), button -> {
                    this.enabled = !this.enabled;
                    button.setMessage(enabledLabel());
                }).bounds(this.left, this.top + 14, this.panelWidth, 20).build());

        this.volumeSlider = this.addRenderableWidget(
                new VolumeSlider(this.left, this.top + 38, this.panelWidth, 20, this.volume));

        int rowY = this.top + 84;
        for (int index = 0; index < 4; index++) {
            final int slot = index;
            EditBox field = new EditBox(this.font, fieldX, rowY + 2, fieldWidth, 18,
                    Component.translatable(TIER_KEYS[index]));
            field.setMaxLength(20);
            field.setResponder(value -> this.onTierChanged(slot, value));
            field.setValue(Long.toString(this.tiers[index]));
            this.tierFields[index] = this.addRenderableWidget(field);
            rowY += ROW_HEIGHT;
        }

        int buttonsY = rowY + 18;
        int halfWidth = this.panelWidth / 2 - 4;
        this.doneButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                        .bounds(this.left, buttonsY, halfWidth, 20).build());
        this.addRenderableWidget(
                Button.builder(Component.translatable("paymentfx.config.reset"), button -> this.reset())
                        .bounds(this.left + this.panelWidth - halfWidth, buttonsY, halfWidth, 20).build());
        this.updateStatus();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, this.top, 0xFFFFFFFF);
        context.drawString(this.font, Component.translatable("paymentfx.config.thresholds"),
                this.left + 4, this.top + 68, 0xFFFFD24A, false);

        int rowY = this.top + 88;
        for (String key : TIER_KEYS) {
            context.drawString(this.font, Component.translatable(key), this.left + 4, rowY, 0xFFB9C6BD, false);
            rowY += ROW_HEIGHT;
        }
        if (!this.status.getString().isEmpty()) {
            context.drawCenteredString(this.font, this.status, this.width / 2, this.top + 180, 0xFFFF6B6B);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        PaymentFxConfig config = PaymentFxConfig.get();
        config.enabled = this.enabled;
        config.masterVolume = this.volume;
        config.tier2Min = this.tiers[0];
        config.tier3Min = this.tiers[1];
        config.tier4Min = this.tiers[2];
        config.tier5Min = this.tiers[3];
        PaymentFxConfig.normalize(config);
        PaymentFxConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private Component enabledLabel() {
        return Component.literal("Enable PaymentFX: " + (this.enabled ? "ON" : "OFF"));
    }

    private void onTierChanged(int index, String raw) {
        long value = parseAmount(raw);
        this.valid[index] = value > 0L;
        if (value > 0L) {
            this.tiers[index] = value;
        }
        this.updateStatus();
    }

    private void updateStatus() {
        boolean allValid = true;
        for (boolean flag : this.valid) {
            allValid &= flag;
        }
        this.status = allValid ? Component.empty() : Component.translatable("paymentfx.config.invalid");
        if (this.doneButton != null) {
            this.doneButton.active = allValid;
        }
    }

    private void reset() {
        this.enabled = true;
        this.volume = 1.0F;
        if (this.enabledButton != null) {
            this.enabledButton.setMessage(enabledLabel());
        }
        if (this.volumeSlider != null) {
            this.volumeSlider.reset();
        }
        for (int index = 0; index < 4; index++) {
            this.tiers[index] = DEFAULTS[index];
            this.valid[index] = true;
            if (this.tierFields[index] != null) {
                this.tierFields[index].setValue(Long.toString(DEFAULTS[index]));
            }
        }
        this.updateStatus();
    }

    /** Accepts "10000000", "10M", "1.5B", "500K" and friends. Returns -1 when unparseable. */
    static long parseAmount(String raw) {
        if (raw == null) {
            return -1L;
        }
        String text = raw.trim().replace(",", "").replace("_", "").replace(" ", "");
        if (text.startsWith("$")) {
            text = text.substring(1);
        }
        if (text.isEmpty()) {
            return -1L;
        }
        double multiplier = 1.0D;
        int cut = text.length();
        switch (Character.toUpperCase(text.charAt(text.length() - 1))) {
            case 'K' -> {
                multiplier = 1_000D;
                cut--;
            }
            case 'M' -> {
                multiplier = 1_000_000D;
                cut--;
            }
            case 'B' -> {
                multiplier = 1_000_000_000D;
                cut--;
            }
            case 'T' -> {
                multiplier = 1_000_000_000_000D;
                cut--;
            }
            default -> {
            }
        }
        if (cut <= 0) {
            return -1L;
        }
        try {
            double value = Double.parseDouble(text.substring(0, cut)) * multiplier;
            if (!Double.isFinite(value) || value < 1.0D) {
                return -1L;
            }
            return (long) Math.min(value, Long.MAX_VALUE / 2.0D);
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }

    private final class VolumeSlider extends AbstractSliderButton {
        VolumeSlider(int x, int y, int width, int height, double value) {
            super(x, y, width, height, Component.empty(), Mth.clamp(value, 0.0D, 1.0D));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal("Master volume: " + Math.round(this.value * 100.0D) + "%"));
        }

        @Override
        protected void applyValue() {
            VanillaConfigScreen.this.volume = (float) this.value;
        }

        void reset() {
            this.value = 1.0D;
            VanillaConfigScreen.this.volume = 1.0F;
            this.updateMessage();
        }
    }
}
