package com.amethyst.client.mixin;

import com.amethyst.client.module.CameraState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void amethyst$look(double dx, double dy, CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player || !CameraState.active()) return;
        CameraState.yaw += (float) dx * 0.15f;
        CameraState.pitch = MathHelper.clamp(CameraState.pitch + (float) dy * 0.15f, -90f, 90f);
        ci.cancel();
    }
}
