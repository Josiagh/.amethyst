package com.amethyst.client.mixin;

import com.amethyst.client.module.CameraState;
import com.amethyst.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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
            var p = MinecraftClient.getInstance().player;
            if (p != null) {
                double yr = Math.toRadians(CameraState.yaw);
                double pr = Math.toRadians(CameraState.pitch);
                double fx = -Math.sin(yr) * Math.cos(pr);
                double fy = -Math.sin(pr);
                double fz = Math.cos(yr) * Math.cos(pr);
                Vec3d eye = p.getLerpedPos(td).add(0, p.getStandingEyeHeight(), 0);
                setRotation(CameraState.yaw, CameraState.pitch);
                setPos(eye.x - fx * 4, eye.y - fy * 4, eye.z - fz * 4);
                this.thirdPerson = true;
            }
        }
    }
}
