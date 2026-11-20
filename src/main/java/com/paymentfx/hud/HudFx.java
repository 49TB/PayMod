package com.paymentfx.hud;

import com.paymentfx.config.PaymentFxConfig;
import com.paymentfx.effect.ActiveEffect;
import com.paymentfx.effect.EffectManager;
import com.paymentfx.effect.Tier;
import com.paymentfx.effect.Timelines;
import com.paymentfx.util.Easing;
import com.paymentfx.util.Skins;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Every 2D overlay: toasts, the gold banner, the slot machine, the nuke warning and the finale. */
public final class HudFx {
    private static final int TOAST_WIDTH = 232;
    private static final int TOAST_HEIGHT = 62;
    private static final int MAX_TOASTS = 3;

    private HudFx() {
    }

    public static void render(GuiGraphics context, DeltaTracker tickCounter) {
        PaymentFxConfig config = PaymentFxConfig.get();
        if (!config.enabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui) {
            return;
        }
        List<ActiveEffect> effects = EffectManager.INSTANCE.effects();
        if (effects.isEmpty()) {
            return;
        }
        Font renderer = client.font;
        int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();
        long now = System.currentTimeMillis();

        int toastIndex = 0;
        for (int i = effects.size() - 1; i >= 0 && toastIndex < MAX_TOASTS; i--) {
            ActiveEffect effect = effects.get(i);
            if (effect.tier == Tier.TOAST) {
                renderToast(context, renderer, effect, now, screenWidth, toastIndex);
                toastIndex++;
            }
        }

        ActiveEffect major = EffectManager.INSTANCE.newest(Tier.TOAST);
        if (major == null) {
            return;
        }
        switch (major.tier) {
            case BANNER -> renderBanner(context, renderer, major, now, screenWidth);
            case SLOTS -> SlotMachineRenderer.render(context, renderer, major, now, screenWidth);
            case NUKE -> renderNuke(context, renderer, major, now, screenWidth);
            case BLACK_HOLE -> renderBlackHole(context, renderer, major, now, screenWidth);
            default -> {
            }
        }
        renderFlash(context, major, now, screenWidth, screenHeight);
    }

    // ------------------------------------------------------------------ tier 1

    private static void renderToast(GuiGraphics context, Font renderer, ActiveEffect effect, long now,
                                    int screenWidth, int index) {
        long elapsed = effect.elapsed(now);
        float in = Easing.clamp01(elapsed / 280.0F);
        float out = Easing.clamp01((effect.durationMs - elapsed) / 520.0F);
        float alpha = Math.min(in, out);
        if (alpha <= 0.02F) {
            return;
        }
        float slide = (1.0F - Easing.outCubic(in)) + (1.0F - out);
        int x = screenWidth - TOAST_WIDTH - 8 + Math.round(slide * (TOAST_WIDTH + 24));
        int y = 10 + index * (TOAST_HEIGHT + 8);

        context.fill(x - 2, y - 2, x + TOAST_WIDTH + 2, y + TOAST_HEIGHT + 2, Draw.withAlpha(0x223BE07A, alpha));
        context.fill(x, y, x + TOAST_WIDTH, y + TOAST_HEIGHT, Draw.withAlpha(0xE60B120E, alpha));
        context.fill(x, y, x + TOAST_WIDTH, y + 1, Draw.withAlpha(0x55275C3A, alpha));
        context.fill(x, y + TOAST_HEIGHT - 1, x + TOAST_WIDTH, y + TOAST_HEIGHT, Draw.withAlpha(0x55275C3A, alpha));
        context.fill(x, y + 2, x + 3, y + TOAST_HEIGHT - 2, Draw.withAlpha(0xFF35E06B, alpha));
        context.fill(x - 3, y + 6, x, y + TOAST_HEIGHT - 6, Draw.withAlpha(0x3310FF6E, alpha));

        if (alpha > 0.25F) {
            Draw.skinFigure(context, Skins.texture(effect.sender), x + 12, y + 8, 1.42F);
        }

        String name = Draw.trim(renderer, effect.sender, TOAST_WIDTH - 56);
        context.drawString(renderer, name, x + 44, y + 15, Draw.withAlpha(0xFFFFD24A, alpha), true);
        context.drawString(renderer, "paid " + effect.amountText, x + 44, y + 29, Draw.withAlpha(0xFFFFFFFF, alpha), true);
        context.drawString(renderer, "received", x + 44, y + 43, Draw.withAlpha(0x8899AFA0, alpha), false);
    }

    // ------------------------------------------------------------------ tier 2

    private static void renderBanner(GuiGraphics context, Font renderer, ActiveEffect effect, long now,
                                     int screenWidth) {
        long elapsed = effect.elapsed(now);
        float in = Easing.clamp01(elapsed / 320.0F);
        float out = Easing.clamp01((effect.durationMs - elapsed) / 520.0F);
        float alpha = Math.min(in, out);
        if (alpha <= 0.02F) {
            return;
        }
        float slide = 1.0F - Easing.outCubic(in);
        float nameScale = Draw.fitScale(renderer, effect.sender, 2.0F, screenWidth - 90);
        int nameWidth = Math.round(renderer.width(effect.sender) * nameScale);
        String line = "paid you " + effect.amountText;
        int panelWidth = Math.max(280, Math.max(nameWidth, renderer.width(line)) + 56);
        int panelHeight = 56;
        int x = (screenWidth - panelWidth) / 2;
        int y = 14 - Math.round(slide * 60.0F);

        context.fill(x - 2, y - 2, x + panelWidth + 2, y + panelHeight + 2, Draw.withAlpha(0x33FFB02E, alpha * 0.6F));
        context.fillGradient(x, y, x + panelWidth, y + panelHeight,
                Draw.withAlpha(0xE61A1206, alpha * 0.92F), Draw.withAlpha(0xD10A0803, alpha * 0.88F));
        int gold = Draw.withAlpha(0xFFFFC43C, alpha);
        context.fill(x, y, x + panelWidth, y + 1, gold);
        context.fill(x, y + panelHeight - 1, x + panelWidth, y + panelHeight, gold);
        context.fill(x, y + 1, x + 2, y + panelHeight - 1, Draw.withAlpha(0xFFFFC43C, alpha * 0.5F));
        context.fill(x + panelWidth - 2, y + 1, x + panelWidth, y + panelHeight - 1, Draw.withAlpha(0xFFFFC43C, alpha * 0.5F));

        Draw.gradientTextV(context, renderer, effect.sender, (screenWidth - nameWidth) / 2, y + 8, nameScale,
                Draw.withAlpha(0xFFFFE3A2, alpha), Draw.withAlpha(0xFFFF9A16, alpha), true);
        context.drawCenteredString(renderer, Component.literal(line), screenWidth / 2,
                y + 10 + Math.round(renderer.lineHeight * nameScale) + 2, Draw.withAlpha(0xFFFFFFFF, alpha));
    }

    // ------------------------------------------------------------------ tier 4

    private static void renderNuke(GuiGraphics context, Font renderer, ActiveEffect effect, long now,
                                   int screenWidth) {
        long elapsed = effect.elapsed(now);
        if (elapsed < Timelines.NUKE_FALL_END + 250L) {
            float alpha = Easing.clamp01(elapsed / 240.0F);
            float blink = Easing.wave(now, 320.0D);
            int flashColor = Draw.mix(0xFFFFFFFF, 0xFFFF3B30, blink);
            String title = "\u26A0 NUKE INCOMING \u26A0";
            float scale = Draw.fitScale(renderer, title, 2.4F, screenWidth - 130);
            int titleWidth = Math.round(renderer.width(title) * scale);
            int centerX = screenWidth / 2;
            int y = 14;

            context.fill(centerX - titleWidth / 2 - 30, y - 8, centerX + titleWidth / 2 + 30, y + 46,
                    Draw.withAlpha(0xA6000000, alpha * 0.8F));
            context.fill(centerX - titleWidth / 2 - 30, y - 8, centerX + titleWidth / 2 + 30, y - 7,
                    Draw.withAlpha(flashColor, alpha));
            context.fill(centerX - titleWidth / 2 - 30, y + 45, centerX + titleWidth / 2 + 30, y + 46,
                    Draw.withAlpha(flashColor, alpha));

            int triangleColor = Draw.withAlpha(flashColor, alpha * (0.45F + blink * 0.55F));
            warningTriangle(context, centerX - titleWidth / 2 - 16, y + 4, 14, triangleColor);
            warningTriangle(context, centerX + titleWidth / 2 + 16, y + 4, 14, triangleColor);

            Draw.textCentered(context, renderer, title, centerX, y + 2, scale, Draw.withAlpha(flashColor, alpha), true);
            context.drawCenteredString(renderer, Component.literal("from " + effect.sender), centerX, y + 30,
                    Draw.withAlpha(0xFFFFFFFF, alpha));
        }

        if (elapsed >= Timelines.NUKE_TEXT_FROM) {
            float alpha = Easing.clamp01((elapsed - Timelines.NUKE_TEXT_FROM) / 420.0F);
            float scale = Draw.fitScale(renderer, effect.sender, 2.4F, screenWidth - 70);
            int nameWidth = Math.round(renderer.width(effect.sender) * scale);
            Draw.gradientTextV(context, renderer, effect.sender, (screenWidth - nameWidth) / 2, 26, scale,
                    Draw.withAlpha(0xFFFFE0A0, alpha), Draw.withAlpha(0xFFFF7A1A, alpha), true);
            context.drawCenteredString(renderer, Component.literal("paid you " + effect.amountText), screenWidth / 2,
                    30 + Math.round(renderer.lineHeight * scale) + 4, Draw.withAlpha(0xFFFFFFFF, alpha));
        }
    }

    private static void warningTriangle(GuiGraphics context, int centerX, int topY, int size, int color) {
        for (int row = 0; row < size; row++) {
            int half = row / 2;
            context.fill(centerX - half, topY + row, centerX + half + 1, topY + row + 1, color);
        }
    }

    // ------------------------------------------------------------------ tier 5

    private static void renderBlackHole(GuiGraphics context, Font renderer, ActiveEffect effect, long now,
                                        int screenWidth) {
        long elapsed = effect.elapsed(now);
        if (elapsed < Timelines.BH_TEXT_FROM) {
            float pulse = 0.12F + 0.1F * Easing.wave(now, 900.0D);
            context.fillGradient(0, 0, screenWidth, 26, Draw.withAlpha(0x66B36BFF, pulse), 0x00000000);
            return;
        }
        float alpha = Easing.clamp01((elapsed - Timelines.BH_TEXT_FROM) / 480.0F);
        String line = effect.sender + " paid you " + effect.amountText;
        float scale = Draw.fitScale(renderer, line, 2.8F, screenWidth - 50);
        int lineWidth = Math.round(renderer.width(line) * scale);
        int x = (screenWidth - lineWidth) / 2;
        int y = 30;

        for (int i = 0; i < 12; i++) {
            float phase = ((now % 1_600L) / 1_600.0F + i * 0.137F) % 1.0F;
            int seed = (int) (effect.id * 31 + i * 7);
            int sparkleX = x + Math.round(Easing.hash(seed) * lineWidth);
            int sparkleY = y - 12 + (int) Math.round(Easing.hash(seed + 1) * 34.0F + Math.sin(phase * Math.PI * 2.0D) * 6.0F);
            int color = Draw.hsv(now * 0.22F + i * 37.0F, 0.75F, 1.0F);
            Draw.sparkle(context, sparkleX, sparkleY, 2 + Math.round(2.0F * (float) Math.sin(phase * Math.PI)), Draw.withAlpha(color, alpha * 0.9F));
        }

        Draw.rainbowText(context, renderer, line, x, y, scale, now, 24.0F, 0.22F, true);
    }

    // ------------------------------------------------------------------ flash

    private static void renderFlash(GuiGraphics context, ActiveEffect effect, long now, int screenWidth, int screenHeight) {
        long elapsed = effect.elapsed(now);
        if (effect.tier == Tier.NUKE) {
            long start = Timelines.NUKE_FALL_END;
            long duration = 1_200L;
            if (elapsed < start || elapsed > start + duration) {
                return;
            }
            float p = (elapsed - start) / (float) duration;
            float alpha = p < 0.12F ? p / 0.12F : 1.0F - (p - 0.12F) / 0.88F;
            int color = Draw.mix(0xFFFFFFFF, 0xFFFF8A1C, Math.min(1.0F, p * 2.0F));
            context.fill(0, 0, screenWidth, screenHeight, Draw.withAlpha(color, alpha * 0.92F));
        } else if (effect.tier == Tier.BLACK_HOLE) {
            long start = Timelines.BH_COLLAPSE_END;
            long duration = 380L;
            if (elapsed < start || elapsed > start + duration) {
                return;
            }
            float p = (elapsed - start) / (float) duration;
            float alpha = 1.0F - p;
            int color = Draw.mix(0xFFF3E0FF, 0xFFB36BFF, p);
            context.fill(0, 0, screenWidth, screenHeight, Draw.withAlpha(color, alpha * 0.7F));
        }
    }
}
