package com.paymentfx.effect;

import com.paymentfx.util.Easing;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;

/**
 * All 3D world work: particles, the client-only nuke model, the black hole, target reticles and
 * the camera shake amplitude. Nothing in this class writes to the world - particles are spawned
 * on the client {@link ClientWorld} only and every mesh is drawn from scratch each frame, so
 * there is no state to leak when an effect ends.
 */
public final class WorldFx {
    private static final Random RANDOM = new Random();

    public static float shakeYaw;
    public static float shakePitch;
    public static boolean hasShake;

    private WorldFx() {
    }

    // ------------------------------------------------------------------ ticking

    public static void tick(MinecraftClient client, ActiveEffect effect, long now) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            return;
        }
        long elapsed = effect.elapsed(now);
        switch (effect.tier) {
            case BANNER -> tickFireworks(client, world, effect, elapsed);
            case NUKE -> tickNuke(client, world, effect, elapsed, now);
            case BLACK_HOLE -> tickBlackHole(client, world, effect, elapsed, now);
            default -> {
            }
        }
    }

    private static void tickFireworks(MinecraftClient client, ClientWorld world, ActiveEffect effect, long elapsed) {
        for (int i = 0; i < Timelines.FIREWORK_BURSTS.length; i++) {
            if (elapsed >= Timelines.FIREWORK_BURSTS[i] && effect.markOnce(10 + i)) {
                fireworkBurst(world, senderAnchor(client, effect.sender).add(0.0, 1.4, 0.0));
            }
        }
    }

    private static void tickNuke(MinecraftClient client, ClientWorld world, ActiveEffect effect, long elapsed, long now) {
        if (effect.target == null) {
            effect.target = groundTarget(client);
            effect.anchor = effect.target.add(0.0, 46.0, 0.0);
        }
        Vec3d target = effect.target;
        Vec3d anchor = effect.anchor;

        if (elapsed < Timelines.NUKE_FALL_END) {
            float progress = Easing.inQuad(elapsed / (float) Timelines.NUKE_FALL_END);
            Vec3d pos = anchor.add(target.subtract(anchor).multiply(progress));
            if (effect.every(now, 55L)) {
                world.addParticle(ParticleTypes.CLOUD, pos.x + jitter(0.35), pos.y + 2.4, pos.z + jitter(0.35), 0.0, -0.02, 0.0);
                world.addParticle(ParticleTypes.LARGE_SMOKE, pos.x + jitter(0.3), pos.y + 2.8, pos.z + jitter(0.3), 0.0, 0.03, 0.0);
                world.addParticle(ParticleTypes.FLAME, pos.x + jitter(0.12), pos.y - 1.2, pos.z + jitter(0.12), 0.0, -0.12, 0.0);
            }
        } else if (elapsed < Timelines.NUKE_FALL_END + 4_600L) {
            if (effect.markOnce(1)) {
                nukeImpact(world, target);
            }
            if (effect.every(now, 70L)) {
                mushroomCloud(world, target, (elapsed - Timelines.NUKE_FALL_END) / 4_600.0F);
            }
        } else if (elapsed < effect.durationMs - 1_200L && effect.every(now, 180L)) {
            world.addParticle(ParticleTypes.LARGE_SMOKE, target.x + jitter(3.0), target.y + 1.0 + RANDOM.nextDouble() * 2.0,
                    target.z + jitter(3.0), jitter(0.02), 0.06, jitter(0.02));
        }
    }

    private static void tickBlackHole(MinecraftClient client, ClientWorld world, ActiveEffect effect, long elapsed, long now) {
        if (effect.anchor == null) {
            effect.anchor = frontOfCamera(client, 7.5D);
        }
        Vec3d center = effect.anchor;

        if (elapsed < Timelines.BH_COLLAPSE_END) {
            float progress = Easing.clamp01(elapsed / (float) Timelines.BH_COLLAPSE_END);
            double radius = 4.6D * (1.0D - 0.8D * progress);
            for (int i = 0; i < 7; i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2.0D + progress * 6.0D;
                double r = radius * (0.45D + RANDOM.nextDouble() * 0.9D);
                Vec3d pos = center.add(Math.cos(angle) * r, jitter(0.8) * (1.0D - progress * 0.5D), Math.sin(angle) * r);
                Vec3d inward = center.subtract(pos).normalize().multiply(0.18D + progress * 0.75D);
                ParticleEffect type = i % 3 == 0 ? ParticleTypes.END_ROD
                        : (i % 3 == 1 ? ParticleTypes.PORTAL : ParticleTypes.ELECTRIC_SPARK);
                world.addParticle(type, pos.x, pos.y, pos.z, inward.x, inward.y, inward.z);
            }
            if (effect.every(now, 70L)) {
                for (int i = 0; i < 12; i++) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
                    double r = radius * (1.1D + RANDOM.nextDouble() * 0.5D);
                    world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, center.x + Math.cos(angle) * r,
                            center.y + jitter(0.35), center.z + Math.sin(angle) * r * 0.45D, 0.0, 0.0, 0.0);
                    world.addParticle(ParticleTypes.WITCH, center.x + Math.cos(angle) * r * 1.4D,
                            center.y + jitter(1.6), center.z + Math.sin(angle) * r * 1.4D, 0.0, 0.0, 0.0);
                }
            }
        } else {
            if (effect.markOnce(2)) {
                supernova(world, center);
            }
            if (elapsed < Timelines.BH_COLLAPSE_END + 1_600L && effect.every(now, 55L)) {
                for (int i = 0; i < 8; i++) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
                    double speed = 0.5D + RANDOM.nextDouble() * 1.4D;
                    world.addParticle(i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK,
                            center.x + Math.cos(angle) * speed, center.y + jitter(1.2), center.z + Math.sin(angle) * speed,
                            Math.cos(angle) * speed * 0.35D, jitter(0.1), Math.sin(angle) * speed * 0.35D);
                }
            }
        }
    }

    // ------------------------------------------------------------------ particles

    private static void fireworkBurst(ClientWorld world, Vec3d at) {
        for (int i = 0; i < 46; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2.0D;
            double phi = Math.acos(2.0D * RANDOM.nextDouble() - 1.0D);
            double speed = 0.35D + RANDOM.nextDouble() * 0.45D;
            double vx = Math.sin(phi) * Math.cos(theta) * speed;
            double vy = Math.cos(phi) * speed;
            double vz = Math.sin(phi) * Math.sin(theta) * speed;
            world.addParticle(i % 8 == 0 ? ParticleTypes.END_ROD : ParticleTypes.FIREWORK,
                    at.x + vx * 4.0D, at.y + vy * 4.0D, at.z + vz * 4.0D,
                    vx * 0.2D, vy * 0.2D, vz * 0.2D);
        }
        world.addParticle(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 0.0, 0.0, 0.0);
    }

    private static void nukeImpact(ClientWorld world, Vec3d at) {
        world.addParticle(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1.5, at.z, 0.0, 0.0, 0.0);
        for (int i = 0; i < 90; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2.0D;
            double speed = 0.4D + RANDOM.nextDouble() * 1.1D;
            double vx = Math.cos(theta) * speed;
            double vz = Math.sin(theta) * speed;
            double vy = 0.2D + RANDOM.nextDouble() * 0.9D;
            ParticleEffect type = i % 4 == 0 ? ParticleTypes.FIREWORK
                    : (i % 4 == 1 ? ParticleTypes.FLAME : (i % 4 == 2 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD));
            world.addParticle(type, at.x + vx, at.y + 0.4D, at.z + vz, vx * 0.5D, vy, vz * 0.5D);
        }
        for (int i = 0; i < 18; i++) {
            world.addParticle(ParticleTypes.FLASH, at.x + jitter(2.5), at.y + 1.0D + RANDOM.nextDouble() * 2.0D,
                    at.z + jitter(2.5), 0.0, 0.0, 0.0);
        }
        for (int i = 0; i < 30; i++) {
            world.addParticle(ParticleTypes.SMOKE, at.x + jitter(1.5), at.y + 0.3D, at.z + jitter(1.5),
                    jitter(0.05), 0.12D, jitter(0.05));
        }
    }

    private static void mushroomCloud(ClientWorld world, Vec3d at, float progress) {
        float t = Easing.clamp01(progress);
        double radius = 1.5D + t * 4.0D;
        for (int i = 0; i < 3; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
            world.addParticle(ParticleTypes.LARGE_SMOKE,
                    at.x + Math.cos(angle) * radius, at.y + 0.6D + t * 6.0D, at.z + Math.sin(angle) * radius,
                    jitter(0.04), 0.18D + t * 0.1D, jitter(0.04));
        }
        if (t < 0.4F) {
            for (int i = 0; i < 2; i++) {
                world.addParticle(ParticleTypes.FLAME, at.x + jitter(2.0), at.y + 1.0D + RANDOM.nextDouble() * 2.0D,
                        at.z + jitter(2.0), 0.0, 0.08D, 0.0);
            }
        }
    }

    private static void supernova(ClientWorld world, Vec3d center) {
        world.addParticle(ParticleTypes.FLASH, center.x, center.y, center.z, 0.0, 0.0, 0.0);
        world.addParticle(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 0.0, 0.0, 0.0);
        for (int i = 0; i < 140; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2.0D;
            double phi = Math.acos(2.0D * RANDOM.nextDouble() - 1.0D);
            double speed = 0.6D + RANDOM.nextDouble() * 1.8D;
            double vx = Math.sin(phi) * Math.cos(theta) * speed;
            double vy = Math.cos(phi) * speed;
            double vz = Math.sin(phi) * Math.sin(theta) * speed;
            ParticleEffect type = i % 5 == 0 ? ParticleTypes.FIREWORK
                    : (i % 5 == 1 ? ParticleTypes.END_ROD
                    : (i % 5 == 2 ? ParticleTypes.ELECTRIC_SPARK
                    : (i % 5 == 3 ? ParticleTypes.CRIT : ParticleTypes.WITCH)));
            world.addParticle(type, center.x + vx, center.y + vy, center.z + vz, vx * 0.6D, vy * 0.6D, vz * 0.6D);
        }
    }

    // ------------------------------------------------------------------ rendering

    public static void render(WorldRenderContext context) {
        List<ActiveEffect> effects = EffectManager.INSTANCE.effects();
        if (effects.isEmpty()) {
            return;
        }
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider provider = context.consumers();
        Camera camera = context.camera();
        if (matrices == null || provider == null || camera == null) {
            return;
        }
        VertexConsumer buffer = provider.getBuffer(RenderLayer.getDebugQuads());
        Vec3d cameraPos = camera.getPos();
        Vector3f right = camera.getRotation().transform(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f up = camera.getRotation().transform(new Vector3f(0.0F, 1.0F, 0.0F));
        long now = System.currentTimeMillis();

        for (ActiveEffect effect : effects) {
            long elapsed = effect.elapsed(now);
            if (effect.tier == Tier.NUKE) {
                renderNuke(matrices, buffer, cameraPos, effect, elapsed);
            } else if (effect.tier == Tier.BLACK_HOLE) {
                renderBlackHole(matrices, buffer, cameraPos, right, up, effect, elapsed);
            }
        }
    }

    private static void renderNuke(MatrixStack matrices, VertexConsumer buffer, Vec3d cameraPos, ActiveEffect effect, long elapsed) {
        Vec3d target = effect.target;
        if (target == null) {
            return;
        }
        matrices.push();
        matrices.translate(target.x - cameraPos.x, target.y - cameraPos.y, target.z - cameraPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        if (elapsed < Timelines.NUKE_FALL_END + 300L) {
            float pulse = Easing.wave((float) elapsed, 500.0D);
            float radius = 1.7F + pulse * 0.5F;
            groundRing(buffer, matrix, 0.02F, radius, 0.16F, color(200, 255, 45, 35), 48);
            groundRing(buffer, matrix, 0.02F, radius * 1.45F, 0.08F, color(110, 255, 70, 40), 48);
            crosshair(buffer, matrix, radius * 1.7F, 0.03F, color(170, 255, 60, 40));
        }

        if (elapsed < Timelines.NUKE_FALL_END && effect.anchor != null) {
            float progress = Easing.inQuad(elapsed / (float) Timelines.NUKE_FALL_END);
            Vec3d pos = effect.anchor.add(target.subtract(effect.anchor).multiply(progress));
            matrices.push();
            matrices.translate(pos.x - target.x, pos.y - target.y, pos.z - target.z);
            drawNukeModel(buffer, matrices.peek().getPositionMatrix(), elapsed);
            matrices.pop();
        } else if (elapsed >= Timelines.NUKE_FALL_END) {
            renderShockwave(buffer, matrix, elapsed - Timelines.NUKE_FALL_END);
        }

        matrices.pop();
    }

    private static void drawNukeModel(VertexConsumer buffer, Matrix4f matrix, long elapsed) {
        int olive = color(255, 92, 100, 60);
        int oliveDark = color(255, 68, 74, 44);
        int yellow = color(255, 232, 196, 52);
        int black = color(255, 28, 26, 22);
        int hot = color(150, 255, 150, 40);
        float sway = (float) Math.sin(elapsed / 180.0D) * 0.05F;

        box(buffer, matrix, sway, 1.05F, 0.0F, 0.55F, 1.35F, 0.55F, olive);
        box(buffer, matrix, sway, 0.55F, 0.0F, 0.58F, 0.18F, 0.58F, yellow);
        for (int i = 0; i < 3; i++) {
            double angle = i * (Math.PI * 2.0D / 3.0D);
            box(buffer, matrix, sway + (float) Math.cos(angle) * 0.58F, 0.55F,
                    (float) Math.sin(angle) * 0.58F, 0.1F, 0.09F, 0.1F, black);
        }
        box(buffer, matrix, sway, -0.45F, 0.0F, 0.46F, 0.28F, 0.46F, oliveDark);
        box(buffer, matrix, sway, -0.78F, 0.0F, 0.32F, 0.2F, 0.32F, olive);
        box(buffer, matrix, sway, -1.02F, 0.0F, 0.16F, 0.14F, 0.16F, oliveDark);
        box(buffer, matrix, sway, 2.05F, 0.0F, 1.05F, 0.42F, 0.06F, oliveDark);
        box(buffer, matrix, sway, 2.05F, 0.0F, 0.06F, 0.42F, 1.05F, oliveDark);
        box(buffer, matrix, sway, 2.55F, 0.0F, 0.35F, 0.35F, 0.35F, hot);
    }

    private static void renderShockwave(VertexConsumer buffer, Matrix4f matrix, long sinceImpact) {
        if (sinceImpact < 0L || sinceImpact > 2_600L) {
            return;
        }
        for (int ring = 0; ring < 3; ring++) {
            long local = sinceImpact - ring * 260L;
            if (local < 0L || local > 2_000L) {
                continue;
            }
            float p = local / 2_000.0F;
            float radius = 1.5F + p * (14.0F - ring * 3.0F);
            int alpha = Math.round(200.0F * (1.0F - p) * (1.0F - p));
            groundRing(buffer, matrix, 0.06F + ring * 0.01F, radius, 0.35F + 1.4F * (1.0F - p),
                    color(alpha, 255, 140 + ring * 40, 40), 64);
        }
    }

    private static void renderBlackHole(MatrixStack matrices, VertexConsumer buffer, Vec3d cameraPos,
                                       Vector3f right, Vector3f up, ActiveEffect effect, long elapsed) {
        Vec3d center = effect.anchor;
        if (center == null) {
            return;
        }
        matrices.push();
        matrices.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Tilted accretion disk basis (view aligned, squashed on one axis).
        float tilt = 0.35F;
        float ct = (float) Math.cos(tilt);
        float st = (float) Math.sin(tilt);
        Vector3f diskRight = new Vector3f(right).mul(ct).add(new Vector3f(up).mul(st));
        Vector3f diskUp = new Vector3f(up).mul(ct).sub(new Vector3f(right).mul(st)).mul(0.34F);

        if (elapsed < Timelines.BH_COLLAPSE_END) {
            float progress = Easing.clamp01(elapsed / (float) Timelines.BH_COLLAPSE_END);
            float core = 2.5F * (1.0F - 0.55F * progress);

            ring(buffer, matrix, right, up, core * 2.6F, core * 3.5F, color(60, 150, 90, 255), 64);
            ring(buffer, matrix, diskRight, diskUp, core * 1.15F, core * 2.35F, color(175, 255, 132, 26), 72);
            ring(buffer, matrix, diskRight, diskUp, core * 1.02F, core * 1.5F, color(215, 255, 200, 82), 72);
            ring(buffer, matrix, diskRight, diskUp, core * 0.96F, core * 1.1F, color(200, 255, 232, 150), 72);

            double spin = (elapsed / 900.0D) % 1.0D;
            for (int i = 0; i < 7; i++) {
                double angle = spin * Math.PI * 2.0D + i * (Math.PI * 2.0D / 7.0D) + elapsed / 700.0D;
                float r = core * (1.3F + 0.2F * (float) Math.sin(i * 2.3F));
                float hx = diskRight.x * (float) Math.cos(angle) * r + diskUp.x * (float) Math.sin(angle) * r;
                float hy = diskRight.y * (float) Math.cos(angle) * r + diskUp.y * (float) Math.sin(angle) * r;
                float hz = diskRight.z * (float) Math.cos(angle) * r + diskUp.z * (float) Math.sin(angle) * r;
                dot(buffer, matrix, hx, hy, hz, 0.13F, color(235, 255, 222, 140));
            }

            ring(buffer, matrix, right, up, 0.0F, core, color(255, 4, 2, 8), 40);
            ring(buffer, matrix, right, up, core * 0.9F, core * 1.0F, color(110, 42, 10, 70), 40);
        } else {
            long since = elapsed - Timelines.BH_COLLAPSE_END;
            if (since < 1_500L) {
                float p = since / 1_500.0F;
                float radius = 1.0F + p * 22.0F;
                int alpha = Math.round(220.0F * (1.0F - p));
                ring(buffer, matrix, right, up, radius * 0.96F, radius, color(alpha, 255, 255, 255), 80);
                ring(buffer, matrix, right, up, radius * 0.72F, radius * 0.78F,
                        color(Math.round(alpha * 0.8F), 200, 130, 255), 80);
                ring(buffer, matrix, right, up, radius * 0.45F, radius * 0.5F,
                        color(Math.round(alpha * 0.7F), 255, 210, 120), 80);
            }
        }
        matrices.pop();
    }

    // ------------------------------------------------------------------ shake

    /** Recomputes the camera offset for this frame. Called once per frame from the camera mixin. */
    public static void updateShake() {
        long now = System.currentTimeMillis();
        float amplitude = 0.0F;
        for (ActiveEffect effect : EffectManager.INSTANCE.effects()) {
            long elapsed = effect.elapsed(now);
            if (effect.tier == Tier.NUKE) {
                if (elapsed < Timelines.NUKE_FALL_END) {
                    amplitude = Math.max(amplitude, 0.1F + 0.35F * (elapsed / (float) Timelines.NUKE_FALL_END));
                } else {
                    amplitude = Math.max(amplitude, 4.2F * (float) Math.exp(-(elapsed - Timelines.NUKE_FALL_END) / 420.0D));
                }
            } else if (effect.tier == Tier.BLACK_HOLE) {
                if (elapsed < Timelines.BH_COLLAPSE_END) {
                    amplitude = Math.max(amplitude, 0.35F * (elapsed / (float) Timelines.BH_COLLAPSE_END));
                } else {
                    amplitude = Math.max(amplitude, 2.6F * (float) Math.exp(-(elapsed - Timelines.BH_COLLAPSE_END) / 320.0D));
                }
            } else if (effect.tier == Tier.SLOTS) {
                for (long lock : Timelines.SLOT_LOCKS) {
                    long since = elapsed - lock;
                    if (since >= 0L && since < 400L) {
                        amplitude = Math.max(amplitude, 0.45F * (1.0F - since / 400.0F));
                    }
                }
            }
        }
        hasShake = amplitude > 0.01F;
        if (!hasShake) {
            shakeYaw = 0.0F;
            shakePitch = 0.0F;
            return;
        }
        double t = now / 1000.0D;
        shakeYaw = (float) (Math.sin(t * 43.0D) * 0.62D + Math.sin(t * 97.0D) * 0.38D) * amplitude;
        shakePitch = (float) (Math.cos(t * 51.0D) * 0.6D + Math.sin(t * 83.0D) * 0.4D) * amplitude * 0.7F;
    }

    // ------------------------------------------------------------------ helpers

    static Vec3d frontOfCamera(MinecraftClient client, double distance) {
        if (client.player == null) {
            return Vec3d.ZERO;
        }
        Vec3d eye = client.player.getEyePos();
        return eye.add(client.player.getRotationVec(1.0F).multiply(distance));
    }

    private static Vec3d senderAnchor(MinecraftClient client, String name) {
        if (client.world != null) {
            for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                if (!player.getGameProfile().getName().equalsIgnoreCase(name)) {
                    continue;
                }
                Vec3d pos = player.getPos();
                if (client.player == null || pos.squaredDistanceTo(client.player.getPos()) < 48.0D * 48.0D) {
                    return pos.add(0.0, 1.2, 0.0);
                }
            }
        }
        return frontOfCamera(client, 6.0D);
    }

    private static Vec3d groundTarget(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return Vec3d.ZERO;
        }
        Vec3d eye = client.player.getEyePos();
        Vec3d look = client.player.getRotationVec(1.0F);
        Vec3d flat = new Vec3d(look.x, 0.0, look.z);
        if (flat.lengthSquared() < 1.0E-4D) {
            flat = new Vec3d(0.0, 0.0, 1.0);
        }
        flat = flat.normalize().multiply(7.0D);
        double x = eye.x + flat.x;
        double z = eye.z + flat.z;
        int top = client.world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        double y = Math.min(top, client.player.getY() + 1.0D);
        return new Vec3d(x, y + 0.05D, z);
    }

    private static double jitter(double amount) {
        return (RANDOM.nextDouble() - 0.5D) * 2.0D * amount;
    }

    private static int color(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    private static void box(VertexConsumer buffer, Matrix4f matrix, float cx, float cy, float cz,
                            float sx, float sy, float sz, int color) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        float x0 = cx - sx;
        float x1 = cx + sx;
        float y0 = cy - sy;
        float y1 = cy + sy;
        float z0 = cz - sz;
        float z1 = cz + sz;
        quad(buffer, matrix, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, red, green, blue, alpha);
        quad(buffer, matrix, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, red, green, blue, alpha);
        quad(buffer, matrix, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, red, green, blue, alpha);
        quad(buffer, matrix, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, red, green, blue, alpha);
        quad(buffer, matrix, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, red, green, blue, alpha);
        quad(buffer, matrix, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, red, green, blue, alpha);
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, ax, ay, az).color(red, green, blue, alpha).next();
        buffer.vertex(matrix, bx, by, bz).color(red, green, blue, alpha).next();
        buffer.vertex(matrix, cx, cy, cz).color(red, green, blue, alpha).next();
        buffer.vertex(matrix, dx, dy, dz).color(red, green, blue, alpha).next();
    }

    private static void groundRing(VertexConsumer buffer, Matrix4f matrix, float y, float radius,
                                   float thickness, int color, int segments) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        float inner = Math.max(0.0F, radius - thickness);
        float outer = radius + thickness;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0D * i / segments;
            double a1 = Math.PI * 2.0D * (i + 1) / segments;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            quad(buffer, matrix,
                    c0 * inner, y, s0 * inner,
                    c0 * outer, y, s0 * outer,
                    c1 * outer, y, s1 * outer,
                    c1 * inner, y, s1 * inner,
                    red, green, blue, alpha);
        }
    }

    private static void crosshair(VertexConsumer buffer, Matrix4f matrix, float length, float y, int color) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        quad(buffer, matrix, -length, y, -0.04F, length, y, -0.04F, length, y, 0.04F, -length, y, 0.04F,
                red, green, blue, alpha);
        quad(buffer, matrix, -0.04F, y, -length, -0.04F, y, length, 0.04F, y, length, 0.04F, y, -length,
                red, green, blue, alpha);
    }

    private static void ring(VertexConsumer buffer, Matrix4f matrix, Vector3f right, Vector3f up,
                             float inner, float outer, int color, int segments) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0D * i / segments;
            double a1 = Math.PI * 2.0D * (i + 1) / segments;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            offsetVertex(buffer, matrix, right, up, c0 * inner, s0 * inner, red, green, blue, alpha);
            offsetVertex(buffer, matrix, right, up, c0 * outer, s0 * outer, red, green, blue, alpha);
            offsetVertex(buffer, matrix, right, up, c1 * outer, s1 * outer, red, green, blue, alpha);
            offsetVertex(buffer, matrix, right, up, c1 * inner, s1 * inner, red, green, blue, alpha);
        }
    }

    private static void dot(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z,
                            float size, int color) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        quad(buffer, matrix, x - size, y - size, z, x + size, y - size, z, x + size, y + size, z,
                x - size, y + size, z, red, green, blue, alpha);
    }

    private static void offsetVertex(VertexConsumer buffer, Matrix4f matrix, Vector3f right, Vector3f up,
                                     float x, float y, float red, float green, float blue, float alpha) {
        buffer.vertex(matrix,
                        right.x * x + up.x * y,
                        right.y * x + up.y * y,
                        right.z * x + up.z * y)
                .color(red, green, blue, alpha)
                .next();
    }
}
