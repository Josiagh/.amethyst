package com.amethyst.client.mixin;

import com.amethyst.client.module.CameraState;
import com.amethyst.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow private boolean thirdPerson;

    @Inject(method = "update", at = @At("RETURN"))
    private void amethyst$update(CallbackInfo ci) {
        float td = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
        if (ModuleManager.FREECAM.isEnabled()) {
            setPos(MathHelper.lerp(td, CameraState.prevX, CameraState.x),
                   MathHelper.lerp(td, CameraState.prevY, CameraState.y),
                   MathHelper.lerp(td, CameraState.prevZ, CameraState.z));
            setRotation(CameraState.yaw, CameraState.pitch);
            this.thirdPerson = true;
        } else if (ModuleManager.FREELOOK.isEnabled()) {
            setRotation(CameraState.yaw, CameraState.pitch);
        }
    }
}
