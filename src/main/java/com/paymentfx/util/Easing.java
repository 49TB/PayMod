package com.paymentfx.util;

public final class Easing {
    private Easing() {
    }

    public static float clamp01(float value) {
        return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
    }

    public static float clamp01(double value) {
        return clamp01((float) value);
    }

    public static float lerp(float from, float to, float delta) {
        return from + (to - from) * clamp01(delta);
    }

    public static float outCubic(float t) {
        float p = 1.0F - clamp01(t);
        return 1.0F - p * p * p;
    }

    public static float outQuart(float t) {
        float p = 1.0F - clamp01(t);
        return 1.0F - p * p * p * p;
    }

    public static float inQuad(float t) {
        float p = clamp01(t);
        return p * p;
    }

    public static float inOutSine(float t) {
        return (float) (-(Math.cos(Math.PI * clamp01(t)) - 1.0D) / 2.0D);
    }

    /** 0..1 oscillation. */
    public static float wave(float timeMs, double periodMs) {
        return (float) ((Math.sin((timeMs / periodMs) * Math.PI * 2.0D) + 1.0D) * 0.5D);
    }

    /** Deterministic pseudo random in 0..1. */
    public static float hash(int seed) {
        float value = (float) Math.sin(seed * 12.9898D) * 43758.5453F;
        return value - (float) Math.floor(value);
    }
}
