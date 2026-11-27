package com.amethyst.client.module;

public class Freelook extends Module {
    public Freelook() { super("Freelook", Category.RENDER); }

    @Override protected void onEnable() {
        if (mc.player == null || ModuleManager.FREECAM.isEnabled()) return;
        CameraState.yaw = mc.player.getYaw();
        CameraState.pitch = mc.player.getPitch();
    }
}
