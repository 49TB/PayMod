package com.paymentfx.mixin;

import com.paymentfx.effect.WorldFx;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the client-side camera shake for the nuke and black hole without touching world state. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("RETURN"), require = 0)
    private void paymentfx$applyShake(CallbackInfo info) {
        WorldFx.updateShake();
        if (!WorldFx.hasShake) {
            return;
        }
        setRotation(getYaw() + WorldFx.shakeYaw, getPitch() + WorldFx.shakePitch);
    }

    @Shadow
    public abstract float getYaw();

    @Shadow
    public abstract float getPitch();
}
