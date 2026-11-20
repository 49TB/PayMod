package com.paymentfx.mixin;

import com.paymentfx.effect.WorldFx;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the client-side camera shake for the nuke and black hole without touching world state. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "setup", at = @At("RETURN"), require = 0)
    private void paymentfx$applyShake(Level level, Entity entity, boolean detached, boolean thirdPerson,
                                      float partialTick, CallbackInfo info) {
        WorldFx.updateShake();
        if (!WorldFx.hasShake) {
            return;
        }
        setRotation(yRot() + WorldFx.shakeYaw, xRot() + WorldFx.shakePitch);
    }

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    public abstract float xRot();

    @Shadow
    public abstract float yRot();
}
