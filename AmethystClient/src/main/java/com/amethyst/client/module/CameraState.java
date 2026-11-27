package com.amethyst.client.module;

public class CameraState {
    public static double x, y, z, prevX, prevY, prevZ;
    public static float yaw, pitch;

    public static boolean active() {
        return ModuleManager.FREECAM.isEnabled() || ModuleManager.FREELOOK.isEnabled();
    }
}
