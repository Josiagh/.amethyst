package com.amethyst.client.module;

import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Category { MOVEMENT, RENDER, WORLD }

    public final String name;
    public final Category category;
    public final List<BoolSetting> bools = new ArrayList<>();
    public final List<NumSetting> nums = new ArrayList<>();
    public boolean expanded;
    private boolean enabled;
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public Module(String name, Category category) { this.name = name; this.category = category; }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }
    public boolean isEnabled() { return enabled; }
    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
}
