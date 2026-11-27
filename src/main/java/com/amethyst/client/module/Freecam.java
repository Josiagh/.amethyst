package com.amethyst.client.module;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class Freecam extends Module {
    public final NumSetting speed = new NumSetting("Speed", 0.9, 0.1, 3.0);
    public final BoolSetting blockInteract = new BoolSetting("Block Clicks", true);
    public final BoolSetting mine = new BoolSetting("Mine", true);
    private boolean mining;

    public Freecam() {
        super("Freecam", Category.RENDER);
        nums.add(speed);
        bools.add(blockInteract);
        bools.add(mine);
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

    @Override protected void onDisable() { stopMining(); }

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

        mineFromCamera();
    }

    private void stopMining() {
        if (mining && mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
        mining = false;
    }

    private void mineFromCamera() {
        if (!mine.value || mc.currentScreen != null || !mc.options.attackKey.isPressed()) {
            stopMining();
            return;
        }
        double yr = Math.toRadians(CameraState.yaw);
        double pr = Math.toRadians(CameraState.pitch);
        Vec3d dir = new Vec3d(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
        Vec3d start = new Vec3d(CameraState.x, CameraState.y, CameraState.z);
        Vec3d end = start.add(dir.multiply(6.0));

        BlockHitResult hit = mc.world.raycast(new RaycastContext(start, end,
            RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        if (hit.getType() != HitResult.Type.BLOCK) { stopMining(); return; }

        BlockPos pos = hit.getBlockPos();
        // only blocks your real body could normally reach (4.5 blocks)
        if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) > 20.25) { stopMining(); return; }

        if (mc.interactionManager.updateBlockBreakingProgress(pos, hit.getSide()))
            mc.player.swingHand(Hand.MAIN_HAND);
        mining = true;
    }
}
