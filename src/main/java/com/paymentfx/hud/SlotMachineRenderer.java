package com.paymentfx.hud;

import com.paymentfx.effect.ActiveEffect;
import com.paymentfx.effect.Timelines;
import com.paymentfx.util.Easing;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Tier 3: retro casino slot machine with marquee bulbs and three locking reels. */
public final class SlotMachineRenderer {
    private static final String[] SYMBOLS = {"$", "7", "\u2605", "\u25C6"};
    private static final int[] SYMBOL_COLORS = {0xFF4DFF8A, 0xFFFF5555, 0xFFFFD75E, 0xFF7CC8FF};

    private static final int FRAME_W = 252;
    private static final int FRAME_H = 112;
    private static final int REEL_W = 64;
    private static final int REEL_H = 58;
    private static final int REEL_GAP = 12;
    private static final int CELL = 40;

    private SlotMachineRenderer() {
    }

    public static void render(GuiGraphics context, Font renderer, ActiveEffect effect, long now, int screenWidth) {
        long elapsed = effect.elapsed(now);
        float in = Easing.clamp01(elapsed / 300.0F);
        float out = Easing.clamp01((effect.durationMs - elapsed) / 450.0F);
        float alpha = Math.min(in, out);
        if (alpha <= 0.02F) {
            return;
        }
        int x = (screenWidth - FRAME_W) / 2;
        int y = 10 - Math.round((1.0F - Easing.outCubic(in)) * 44.0F);

        // glow + frame
        context.fill(x - 3, y - 3, x + FRAME_W + 3, y + FRAME_H + 3, Draw.withAlpha(0x33FFB02E, alpha));
        Draw.panel(context, x, y, FRAME_W, FRAME_H, Draw.withAlpha(0xFF2A0808, alpha), Draw.withAlpha(0xFFFFC43C, alpha));
        context.fillGradient(x + 4, y + 4, x + FRAME_W - 4, y + FRAME_H - 4,
                Draw.withAlpha(0xFF9E1B1B, alpha * 0.95F), Draw.withAlpha(0xFF4E0808, alpha * 0.95F));

        // marquee bulbs, top and bottom
        for (int i = 0; i < 16; i++) {
            float phase = (float) Math.sin(now * 0.006D + i * 0.55D);
            int bulb = phase > 0.35F ? 0xFFFFF3B0 : (phase > -0.25F ? 0xFFFFC83C : 0xFF7A4A10);
            int bulbX = x + 12 + i * 14;
            context.fill(bulbX - 1, y + 3, bulbX + 5, y + 9, Draw.withAlpha(0x44FFE08A, alpha));
            context.fill(bulbX, y + 4, bulbX + 4, y + 8, Draw.withAlpha(bulb, alpha));
            context.fill(bulbX - 1, y + FRAME_H - 9, bulbX + 5, y + FRAME_H - 3, Draw.withAlpha(0x44FFE08A, alpha));
            context.fill(bulbX, y + FRAME_H - 8, bulbX + 4, y + FRAME_H - 4, Draw.withAlpha(bulb, alpha));
        }

        // JACKPOT header
        String header = "JACKPOT";
        float headerScale = 1.8F;
        int headerWidth = Math.round(renderer.width(header) * headerScale);
        Draw.rainbowText(context, renderer, header, (screenWidth - headerWidth) / 2, y + 12, headerScale, now, 42.0F, 0.3F, true);
        context.drawCenteredString(renderer, Component.literal("~ ~ ~"), screenWidth / 2, y + 30,
                Draw.withAlpha(0xFFFFC43C, alpha * 0.6F));

        // reels
        int reelsY = y + 40;
        int startX = x + (FRAME_W - (3 * REEL_W + 2 * REEL_GAP)) / 2;
        for (int i = 0; i < 3; i++) {
            int reelX = startX + i * (REEL_W + REEL_GAP);
            context.fill(reelX - 2, reelsY - 2, reelX + REEL_W + 2, reelsY + REEL_H + 2,
                    Draw.withAlpha(0xFFFFC43C, alpha * 0.85F));
            context.fill(reelX, reelsY, reelX + REEL_W, reelsY + REEL_H, Draw.withAlpha(0xFF120A0A, alpha));
            boolean locked = elapsed >= Timelines.SLOT_LOCKS[i];

            context.enableScissor(reelX, reelsY, reelX + REEL_W, reelsY + REEL_H);
            if (locked) {
                drawLockedReel(context, renderer, reelX, reelsY, elapsed, alpha);
            } else {
                drawSpinningReel(context, renderer, reelX, reelsY, elapsed, i, alpha);
            }
            context.disableScissor();

            if (elapsed > Timelines.SLOT_LOCKS[i] && elapsed < Timelines.SLOT_LOCKS[i] + 260L) {
                float flash = 1.0F - (elapsed - Timelines.SLOT_LOCKS[i]) / 260.0F;
                context.fill(reelX, reelsY, reelX + REEL_W, reelsY + REEL_H, Draw.withAlpha(0xFFFFFFFF, flash * 0.5F * alpha));
            }
        }

        // payout text under the machine
        String name = effect.sender;
        float nameScale = Draw.fitScale(renderer, name, 2.0F, screenWidth - 70);
        int nameWidth = Math.round(renderer.width(name) * nameScale);
        Draw.gradientTextV(context, renderer, name, (screenWidth - nameWidth) / 2, y + FRAME_H + 10, nameScale,
                Draw.withAlpha(0xFFFFF0C0, alpha), Draw.withAlpha(0xFFE8A21C, alpha), true);
        context.drawCenteredString(renderer, Component.literal("paid you " + effect.amountText), screenWidth / 2,
                y + FRAME_H + 14 + Math.round(renderer.lineHeight * nameScale), Draw.withAlpha(0xFFFFFFFF, alpha));
    }

    private static void drawSpinningReel(GuiGraphics context, Font renderer, int reelX, int reelY,
                                         long elapsed, int index, float alpha) {
        long lockAt = Timelines.SLOT_LOCKS[index];
        float progress = Easing.clamp01(elapsed / (float) lockAt);
        float travel = 1.0F + index * 0.37F + 6.0F * Easing.outCubic(progress);
        int base = (int) Math.floor(travel);
        float fraction = travel - base;
        for (int k = -1; k <= 2; k++) {
            int symbolIndex = Math.floorMod(base + k + index, SYMBOLS.length);
            int centerY = reelY + Math.round((k - fraction + 1.0F) * CELL) + CELL / 2;
            drawSymbol(context, renderer, SYMBOLS[symbolIndex], reelX + REEL_W / 2, centerY, 1.9F,
                    Draw.withAlpha(SYMBOL_COLORS[symbolIndex], alpha));
        }
    }

    private static void drawLockedReel(GuiGraphics context, Font renderer, int reelX, int reelY,
                                       long elapsed, float alpha) {
        context.fill(reelX + 6, reelY + 6, reelX + REEL_W - 6, reelY + REEL_H - 6, Draw.withAlpha(0x77FFD24A, alpha * 0.9F));
        context.fill(reelX + 10, reelY + 10, reelX + REEL_W - 10, reelY + REEL_H - 10, Draw.withAlpha(0xCC16351F, alpha));
        float pulse = 1.0F + 0.12F * Easing.wave(elapsed, 260.0D);
        drawSymbol(context, renderer, "$", reelX + REEL_W / 2, reelY + REEL_H / 2, 2.3F * pulse,
                Draw.withAlpha(0xFF6BFF9A, alpha));
        drawSymbol(context, renderer, "$", reelX + REEL_W / 2, reelY + REEL_H / 2 - 1, 2.3F * pulse,
                Draw.withAlpha(0xFFFFF3B0, alpha * 0.55F));
    }

    private static void drawSymbol(GuiGraphics context, Font renderer, String symbol,
                                   int centerX, int centerY, float scale, int color) {
        context.pose().pushMatrix();
        context.pose().translate(centerX, centerY);
        context.pose().scale(scale, scale);
        int width = renderer.width(symbol);
        context.drawString(renderer, symbol, -width / 2, -renderer.lineHeight / 2, color, true);
        context.pose().popMatrix();
    }
}
