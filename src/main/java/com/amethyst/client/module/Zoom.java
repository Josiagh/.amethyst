package com.amethyst.client.module;

public class Zoom extends Module {
    public final NumSetting fov = new NumSetting("FOV", 30, 30, 90);
    private int saved = 70;

    public Zoom() {
        super("Zoom", Category.RENDER);
        nums.add(fov);
    }

    @Override protected void onEnable() {
        saved = mc.options.getFov().getValue();
    }

    @Override public void onTick() {
        mc.options.getFov().setValue((int) fov.value);
    }

    @Override protected void onDisable() {
        mc.options.getFov().setValue(saved);
    }
}
