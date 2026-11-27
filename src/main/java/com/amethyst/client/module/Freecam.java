package com.amethyst.client.module;

import net.minecraft.util.math.Vec3d;

public class Freecam extends Module {
    public final NumSetting speed = new NumSetting("Speed", 0.9, 0.1, 3.0);
    public final BoolSetting blockInteract = new BoolSetting("Block Clicks", true);

    public Freecam() {
        super("Freecam", Category.RENDER);
        nums.add(speed);
        bools.add(blockInteract);
    }

    @Override protected void onEnable() {
        if (mc.player == null) return;
        Vec3d eye = mc.player.getEyePos();
        CameraState.x = CameraState.prevX = eye.x;
        CameraState.y = CameraState.prevY = eye.y;
        CameraState.z = CameraState.prevZ = eye.z;
        CameraState.yaw = mc.player.getYaw();
        CameraState.pitch = mc.player.getPitch();
    }

    @Override public void onTick() {
        CameraState.prevX = CameraState.x;
        CameraState.prevY = CameraState.y;
        CameraState.prevZ = CameraState.z;

        float yr = (float) Math.toRadians(CameraState.yaw);
        float pr = (float) Math.toRadians(CameraState.pitch);
        Vec3d fwd = new Vec3d(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
        Vec3d right = new Vec3d(-Math.cos(yr), 0, -Math.sin(yr));

        Vec3d move = Vec3d.ZERO;
        if (mc.options.forwardKey.isPressed()) move = move.add(fwd);
        if (mc.options.backKey.isPressed())    move = move.subtract(fwd);
        if (mc.options.rightKey.isPressed())   move = move.add(right);
if (mc.options.leftKey.isPressed())    move = move.subtract(right);
        if (mc.options.jumpKey.isPressed())    move = move.add(0, 1, 0);
        if (mc.options.sneakKey.isPressed())   move = move.add(0, -1, 0);

        if (move.lengthSquared() > 0) move = move.normalize().multiply(speed.value);
        CameraState.x += move.x;
        CameraState.y += move.y;
        CameraState.z += move.z;
    }
}
