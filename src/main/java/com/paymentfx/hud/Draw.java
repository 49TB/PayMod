package com.paymentfx.hud;

import com.paymentfx.util.Easing;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Pixel drawing helpers: gradient text, rainbow text, panels and the 2D player skin figure. */
public final class Draw {
    private Draw() {
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    public static int withAlpha(int color, float multiplier) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Easing.clamp01(multiplier));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public static int mix(int from, int to, float delta) {
        float t = Easing.clamp01(delta);
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return argb(a, r, g, b);
    }

    /** HSV to packed ARGB, full alpha. Hue in degrees. */
    public static int hsv(float hue, float saturation, float value) {
        float h = ((hue % 360.0F) + 360.0F) % 360.0F / 60.0F;
        float c = value * saturation;
        float x = c * (1.0F - Math.abs(h % 2.0F - 1.0F));
        float m = value - c;
        float r;
        float g;
        float b;
        int sector = (int) h;
        switch (sector) {
            case 0 -> {
                r = c;
                g = x;
                b = 0.0F;
            }
            case 1 -> {
                r = x;
                g = c;
                b = 0.0F;
            }
            case 2 -> {
                r = 0.0F;
                g = c;
                b = x;
            }
            case 3 -> {
                r = 0.0F;
                g = x;
                b = c;
            }
            case 4 -> {
                r = x;
                g = 0.0F;
                b = c;
            }
            default -> {
                r = c;
                g = 0.0F;
                b = x;
            }
        }
        return argb(255, Math.round((r + m) * 255.0F), Math.round((g + m) * 255.0F), Math.round((b + m) * 255.0F));
    }

    public static void panel(GuiGraphics context, int x, int y, int width, int height, int fill, int border) {
        context.fill(x, y, x + width, y + height, fill);
        if (((border >>> 24) & 0xFF) > 0) {
            context.fill(x, y, x + width, y + 1, border);
            context.fill(x, y + height - 1, x + width, y + height, border);
            context.fill(x, y + 1, x + 1, y + height - 1, border);
            context.fill(x + width - 1, y + 1, x + width, y + height - 1, border);
        }
    }

    public static void textCentered(GuiGraphics context, Font renderer, String text, int centerX, int y,
                                    float scale, int color, boolean shadow) {
        int width = Math.round(renderer.width(text) * scale);
        context.pose().pushMatrix();
        context.pose().translate(centerX - width / 2.0F, y);
        context.pose().scale(scale, scale);
        context.drawString(renderer, text, 0, 0, color, shadow);
        context.pose().popMatrix();
    }

    /** Horizontal per-character gradient. */
    public static void gradientTextH(GuiGraphics context, Font renderer, String text, int x, int y,
                                     float scale, int from, int to, boolean shadow) {
        context.pose().pushMatrix();
        context.pose().translate(x, y);
        context.pose().scale(scale, scale);
        int cursor = 0;
        int denominator = Math.max(1, text.length() - 1);
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            context.drawString(renderer, character, cursor, 0, mix(from, to, i / (float) denominator), shadow);
            cursor += renderer.width(character);
        }
        context.pose().popMatrix();
    }

    /**
     * True vertical gradient: the glyphs are drawn once per 2px band with a scissor around the band.
     * The scissor is given in absolute GUI coordinates, which is why the transform here is only a
     * translate plus a uniform scale.
     */
    public static void gradientTextV(GuiGraphics context, Font renderer, String text, int x, int y,
                                     float scale, int top, int bottom, boolean shadow) {
        int textWidth = renderer.width(text);
        int height = renderer.lineHeight;
        context.pose().pushMatrix();
        context.pose().translate(x, y);
        context.pose().scale(scale, scale);
        for (int band = 0; band < height; band += 2) {
            int y0 = y + Math.round(band * scale);
            int y1 = y + Math.round(Math.min(height, band + 2) * scale);
            context.enableScissor(x, y0, x + Math.max(1, Math.round(textWidth * scale)) + 1, y1);
            context.drawString(renderer, text, 0, 0, mix(top, bottom, (band + 1.0F) / height), shadow);
            context.disableScissor();
        }
        context.pose().popMatrix();
    }

    /** Horizontal rainbow, animated over time. */
    public static void rainbowText(GuiGraphics context, Font renderer, String text, int x, int y,
                                   float scale, long now, float hueStep, float hueSpeed, boolean shadow) {
        context.pose().pushMatrix();
        context.pose().translate(x, y);
        context.pose().scale(scale, scale);
        int cursor = 0;
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            float hue = (now * hueSpeed + i * hueStep) % 360.0F;
            context.drawString(renderer, character, cursor, 0, hsv(hue, 0.72F, 1.0F), shadow);
            cursor += renderer.width(character);
        }
        context.pose().popMatrix();
    }

    public static float fitScale(Font renderer, String text, float desired, int maxWidth) {
        int width = renderer.width(text);
        if (width <= 0) {
            return desired;
        }
        return Math.max(0.6F, Math.min(desired, maxWidth / (float) width));
    }

    public static String trim(Font renderer, String text, int maxWidth) {
        if (renderer.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (renderer.width(builder.toString() + text.charAt(i) + ellipsis) > maxWidth) {
                break;
            }
            builder.append(text.charAt(i));
        }
        return builder + ellipsis;
    }

    public static void sparkle(GuiGraphics context, int centerX, int centerY, int size, int color) {
        if (size <= 0) {
            return;
        }
        context.fill(centerX - size, centerY, centerX + size + 1, centerY + 1, color);
        context.fill(centerX, centerY - size, centerX + 1, centerY + size + 1, color);
    }

    /** 2D standing figure built from the 64x64 skin layout. */
    public static void skinFigure(GuiGraphics context, Identifier texture, int x, int y, float scale) {
        context.pose().pushMatrix();
        context.pose().translate(x, y);
        context.pose().scale(scale, scale);
        skinPart(context, texture, 4, 0, 8, 8, 8, 8);      // head
        skinPart(context, texture, 4, 8, 20, 20, 8, 12);    // body
        skinPart(context, texture, 0, 8, 44, 20, 4, 12);    // right arm
        skinPart(context, texture, 12, 8, 36, 52, 4, 12);   // left arm
        skinPart(context, texture, 4, 20, 4, 20, 4, 12);    // right leg
        skinPart(context, texture, 8, 20, 20, 52, 4, 12);   // left leg
        skinPart(context, texture, 4, 0, 40, 8, 8, 8);      // hat layer
        context.pose().popMatrix();
    }

    private static void skinPart(GuiGraphics context, Identifier texture, int x, int y,
                                 int u, int v, int width, int height) {
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 64, 64);
    }
}
