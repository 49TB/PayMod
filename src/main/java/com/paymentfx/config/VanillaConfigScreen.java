package com.paymentfx.config;

import com.paymentfx.chat.PaymentParser;
import com.paymentfx.effect.EffectManager;
import com.paymentfx.effect.Tier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The settings screen, built from plain vanilla widgets only: no Cloth Config, no ModMenu-only
 * types. Two paginated tabs keep everything from overlapping: the five tier thresholds with their
 * ON/OFF switches and Test buttons, and the server chat pay format. Every change is written to
 * config/paymentfx.json by "Save &amp; Close".
 */
public final class VanillaConfigScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 440;
    private static final int TIER_COUNT = 5;
    private static final int ROW_HEIGHT = 21;
    private static final int BOTTOM_OFFSET = 172;
    private static final String[] TIER_NAMES = {
            "Toast notification",
            "Fireworks banner",
            "Jackpot slot machine",
            "Tactical nuke",
            "Black hole singularity"
    };
    private static final long[] DEFAULT_THRESHOLDS = {0L, 10_000_000L, 500_000_000L, 1_000_000_000L, 10_000_000_000L};
    private static final String[] EXAMPLES = {
            "{player} paid you ${amount}",
            "[Pay] {player} -> You: ${amount}",
            "You received ${amount} from {player}",
            "{player} has sent you {amount} coins"
    };
    private static final int PAGE_TIERS = 0;
    private static final int PAGE_CHAT = 1;

    private final Screen parent;
    private final long[] thresholds = new long[TIER_COUNT];
    private final boolean[] tierEnabled = new boolean[TIER_COUNT];
    private final boolean[] thresholdValid = new boolean[TIER_COUNT];

    private boolean enabled;
    private float volume;
    private String chatPattern;
    private boolean chatValid;

    private int page = PAGE_TIERS;
    private int left;
    private int top;
    private int tabY;
    private int contentY;
    private int panelWidth;

    private int labelWidth;
    private int fieldWidth;
    private int toggleX;
    private int testX;

    private Button saveButton;
    private Button enabledButton;
    private VolumeSlider volumeSlider;
    private EditBox chatField;
    private String statusKey = "";
    private int statusColor = 0xFFFF6B6B;

    public VanillaConfigScreen(Screen parent) {
        super(Component.literal("PaymentFX Settings"));
        this.parent = parent;
        PaymentFxConfig config = PaymentFxConfig.get();
        this.enabled = config.enabled;
        this.volume = config.masterVolume;
        this.thresholds[0] = config.tier1Min;
        this.thresholds[1] = config.tier2Min;
        this.thresholds[2] = config.tier3Min;
        this.thresholds[3] = config.tier4Min;
        this.thresholds[4] = config.tier5Min;
        this.tierEnabled[0] = config.tier1Enabled;
        this.tierEnabled[1] = config.tier2Enabled;
        this.tierEnabled[2] = config.tier3Enabled;
        this.tierEnabled[3] = config.tier4Enabled;
        this.tierEnabled[4] = config.tier5Enabled;
        this.chatPattern = config.chatPattern;
        for (int i = 0; i < TIER_COUNT; i++) {
            this.thresholdValid[i] = true;
        }
        this.chatValid = PaymentParser.isValid(this.chatPattern);
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(MAX_PANEL_WIDTH, Math.max(200, this.width - 20));
        this.left = (this.width - this.panelWidth) / 2;
        this.top = Math.max(2, (this.height - 224) / 2);
        this.tabY = this.top + 12;
        this.contentY = this.tabY + 20;

        int tabWidth = Math.min(84, (this.panelWidth - 4) / 2);
        int tabX = this.left + (this.panelWidth - tabWidth * 2 - 4) / 2;
        this.addRenderableWidget(Button.builder(Component.literal(tabLabel(PAGE_TIERS, "Tiers")), button -> this.setPage(PAGE_TIERS))
                .bounds(tabX, this.tabY, tabWidth, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal(tabLabel(PAGE_CHAT, "Chat")), button -> this.setPage(PAGE_CHAT))
                .bounds(tabX + tabWidth + 4, this.tabY, tabWidth, 16).build());

        if (this.page == PAGE_TIERS) {
            this.buildTierPage();
        } else {
            this.buildChatPage();
        }

        int bottomY = this.contentY + BOTTOM_OFFSET;
        int saveWidth = Math.max(80, (this.panelWidth - 6) * 3 / 5);
        int resetWidth = this.panelWidth - 6 - saveWidth;
        this.saveButton = this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), button -> this.saveAndClose())
                .bounds(this.left, bottomY, saveWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Reset all"), button -> this.reset())
                .bounds(this.left + this.panelWidth - resetWidth, bottomY, resetWidth, 20).build());

        this.updateStatus();
    }

    private void buildTierPage() {
        this.enabledButton = this.addRenderableWidget(
                Button.builder(enabledLabel(), button -> {
                    this.enabled = !this.enabled;
                    button.setMessage(enabledLabel());
                }).bounds(this.left, this.contentY, this.panelWidth, 20).build());

        this.volumeSlider = this.addRenderableWidget(
                new VolumeSlider(this.left, this.contentY + 22, this.panelWidth, 20, this.volume));

        this.labelWidth = Math.min(150, this.panelWidth * 36 / 100);
        int toggleWidth = 42;
        int testWidth = 50;
        this.fieldWidth = Math.max(50, this.panelWidth - this.labelWidth - toggleWidth - testWidth - 20);
        int fieldX = this.left + this.labelWidth;
        this.toggleX = fieldX + this.fieldWidth + 4;
        this.testX = this.toggleX + toggleWidth + 4;

        int rowY = this.contentY + 56;
        for (int index = 0; index < TIER_COUNT; index++) {
            final int slot = index;
            final Tier tier = Tier.values()[index];
            EditBox field = new EditBox(this.font, fieldX, rowY + 1, this.fieldWidth, 18,
                    Component.literal(TIER_NAMES[index]));
            field.setMaxLength(20);
            field.setResponder(value -> this.onThresholdChanged(slot, value));
            field.setValue(Long.toString(this.thresholds[index]));
            this.addRenderableWidget(field);

            this.addRenderableWidget(Button.builder(toggleLabel(index), button -> {
                this.tierEnabled[slot] = !this.tierEnabled[slot];
                button.setMessage(toggleLabel(slot));
            }).bounds(this.toggleX, rowY, toggleWidth, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Test"), button -> EffectManager.INSTANCE.preview(tier))
                    .bounds(this.testX, rowY, testWidth, 20).build());

            rowY += ROW_HEIGHT;
        }
    }

    private void buildChatPage() {
        int resetWidth = Math.min(70, this.panelWidth / 3);
        int fieldWidth = this.panelWidth - resetWidth - 4;
        this.chatField = new EditBox(this.font, this.left, this.contentY + 12, fieldWidth, 20,
                Component.literal("Chat Pay Format"));
        this.chatField.setMaxLength(96);
        this.chatField.setResponder(value -> {
            this.chatPattern = value;
            this.chatValid = PaymentParser.isValid(value);
            this.updateStatus();
        });
        this.chatField.setValue(this.chatPattern);
        this.addRenderableWidget(this.chatField);

        this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            this.chatPattern = PaymentFxConfig.DEFAULT_CHAT_PATTERN;
            this.chatValid = true;
            if (this.chatField != null) {
                this.chatField.setValue(this.chatPattern);
            }
            this.updateStatus();
        }).bounds(this.left + this.panelWidth - resetWidth, this.contentY + 12, resetWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Exactly one background pass: the widgets are drawn by super.render afterwards.
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, this.top, 0xFFFFFFFF);

        if (this.page == PAGE_TIERS) {
            context.drawString(this.font, Component.literal("Minimum"), this.left + this.labelWidth,
                    this.contentY + 46, 0xFFFFD24A, false);
            context.drawString(this.font, Component.literal("On"), this.toggleX + 12,
                    this.contentY + 46, 0xFFFFD24A, false);
            context.drawString(this.font, Component.literal("Preview"), this.testX + 1,
                    this.contentY + 46, 0xFFFFD24A, false);

            int rowY = this.contentY + 58;
            for (int index = 0; index < TIER_COUNT; index++) {
                Tier tier = Tier.values()[index];
                int color = this.tierEnabled[index] ? 0xFFFFF3B0 : 0xFF6E7A72;
                context.drawString(this.font, "T" + tier.number + " " + TIER_NAMES[index], this.left + 2, rowY + 5, color, false);
                rowY += ROW_HEIGHT;
            }
            drawStatus(context, this.contentY + 161);
        } else {
            context.drawString(this.font, Component.literal("Chat Pay Format"), this.left + 2,
                    this.contentY, 0xFFFFD24A, false);
            context.drawString(this.font, Component.literal("Use {player} for the sender and {amount} for the value."),
                    this.left + 2, this.contentY + 38, 0xFF8A9A90, false);
            context.drawString(this.font, Component.literal("Examples:"), this.left + 2,
                    this.contentY + 52, 0xFF8A9A90, false);
            int exampleY = this.contentY + 64;
            for (String example : EXAMPLES) {
                context.drawString(this.font, Component.literal("  " + example), this.left + 2, exampleY, 0xFFB9C6BD, false);
                exampleY += 11;
            }
            context.drawString(this.font, Component.literal(
                            "Formatting codes are stripped before matching."), this.left + 2,
                    exampleY + 4, 0xFF8A9A90, false);
            drawStatus(context, this.contentY + 130);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawStatus(GuiGraphics context, int y) {
        if (!this.statusKey.isEmpty()) {
            context.drawCenteredString(this.font, Component.literal(this.statusKey), this.width / 2, y, this.statusColor);
        }
    }

    @Override
    public void onClose() {
        if (this.statusKey.isEmpty()) {
            this.apply();
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private void saveAndClose() {
        if (!this.statusKey.isEmpty()) {
            return;
        }
        this.apply();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private void apply() {
        PaymentFxConfig config = PaymentFxConfig.get();
        config.enabled = this.enabled;
        config.masterVolume = this.volume;
        config.tier1Min = this.thresholds[0];
        config.tier2Min = this.thresholds[1];
        config.tier3Min = this.thresholds[2];
        config.tier4Min = this.thresholds[3];
        config.tier5Min = this.thresholds[4];
        config.tier1Enabled = this.tierEnabled[0];
        config.tier2Enabled = this.tierEnabled[1];
        config.tier3Enabled = this.tierEnabled[2];
        config.tier4Enabled = this.tierEnabled[3];
        config.tier5Enabled = this.tierEnabled[4];
        config.chatPattern = this.chatPattern;
        PaymentFxConfig.normalize(config);
        PaymentFxConfig.save();
    }

    private void setPage(int page) {
        if (this.page == page) {
            return;
        }
        this.page = page;
        // Fields of the page being left are rebuilt from the last valid values, so no stale
        // "invalid input" flag may keep Save disabled with no field left to fix it.
        for (int i = 0; i < TIER_COUNT; i++) {
            this.thresholdValid[i] = true;
        }
        this.clearWidgets();
        this.init();
    }

    private String tabLabel(int page, String label) {
        return this.page == page ? "[ " + label + " ]" : label;
    }

    private Component enabledLabel() {
        return Component.literal("Enable PaymentFX: " + (this.enabled ? "ON" : "OFF"));
    }

    private Component toggleLabel(int index) {
        return Component.literal(this.tierEnabled[index] ? "ON" : "OFF");
    }

    private void onThresholdChanged(int index, String raw) {
        long value = parseThreshold(raw, index == 0);
        this.thresholdValid[index] = value >= 0L;
        if (value >= 0L) {
            this.thresholds[index] = value;
        }
        this.updateStatus();
    }

    private void updateStatus() {
        boolean parsed = true;
        for (int i = 0; i < TIER_COUNT; i++) {
            parsed &= this.thresholdValid[i];
        }
        boolean ordered = true;
        for (int i = 1; i < TIER_COUNT; i++) {
            if (this.thresholds[i] <= this.thresholds[i - 1]) {
                ordered = false;
            }
        }
        if (!parsed) {
            this.statusKey = "Enter a number, optionally with K, M, B or T (for example 10M).";
        } else if (!ordered) {
            this.statusKey = "Each tier must start above the tier below it.";
        } else if (!this.chatValid) {
            this.statusKey = "The chat format needs both {player} and {amount}.";
        } else {
            this.statusKey = "";
        }
        if (this.saveButton != null) {
            this.saveButton.active = this.statusKey.isEmpty();
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
        for (int index = 0; index < TIER_COUNT; index++) {
            this.thresholds[index] = DEFAULT_THRESHOLDS[index];
            this.tierEnabled[index] = true;
            this.thresholdValid[index] = true;
        }
        this.chatPattern = PaymentFxConfig.DEFAULT_CHAT_PATTERN;
        this.chatValid = true;
        if (this.chatField != null) {
            this.chatField.setValue(this.chatPattern);
        }
        this.page = PAGE_TIERS;
        this.clearWidgets();
        this.init();
    }

    /** Accepts "10000000", "10M", "1.5B", "500K" and friends. Returns -1 when unparseable. */
    static long parseThreshold(String raw, boolean allowZero) {
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
            if (!Double.isFinite(value) || value < 0.0D) {
                return -1L;
            }
            if (value < 1.0D && !allowZero) {
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
