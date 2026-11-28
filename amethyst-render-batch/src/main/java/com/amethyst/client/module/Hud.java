package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Watermark plus a list of enabled modules. */
public class Hud extends Module {
    public final BoolSetting watermark = new BoolSetting("Watermark", true);
    public final BoolSetting moduleList = new BoolSetting("Module List", true);

    public Hud() {
        super("Hud", Category.RENDER);
        bools.add(watermark);
        bools.add(moduleList);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (isEnabled()) draw(ctx);
        });
    }

    private void draw(DrawContext ctx) {
        if (mc.player == null) return;
        if (watermark.value) ctx.drawTextWithShadow(mc.textRenderer, "Amethyst", 4, 4, 0xFFB388FF);
        if (!moduleList.value) return;

        List<Module> on = new ArrayList<>();
        for (Module m : ModuleManager.modules) if (m.isEnabled()) on.add(m);
        on.sort(Comparator.comparingInt((Module m) -> mc.textRenderer.getWidth(m.name)).reversed());

        int w = mc.getWindow().getScaledWidth();
        int y = 4;
        for (Module m : on) {
            int tw = mc.textRenderer.getWidth(m.name);
            ctx.fill(w - tw - 8, y - 1, w - 2, y + 10, 0x80000000);
            ctx.fill(w - 3, y - 1, w - 2, y + 10, 0xFF8E24AA);
            ctx.drawTextWithShadow(mc.textRenderer, m.name, w - tw - 6, y, 0xFFE1BEE7);
            y += 11;
        }
    }
}
