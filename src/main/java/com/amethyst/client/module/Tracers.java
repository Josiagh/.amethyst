package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class Tracers extends Module {
    public final NumSetting range = new NumSetting("Range", 256, 16, 1024);

    public Tracers() {
        super("Tracers", Category.RENDER);
        nums.add(range);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (isEnabled()) draw(ctx, tc.getTickProgress(true));
        });
    }

    private void draw(DrawContext ctx, float td) {
        if (mc.player == null || mc.world == null) return;
        Overlay3D.View v = Overlay3D.view(td);
        for (PlayerEntity p : RenderUtil.players(range.value)) {
            if (p == mc.player) continue;
            Vec3d chest = RenderUtil.pos(p, td).add(0, p.getHeight() * 0.6, 0);
            int col = RenderUtil.distColor(mc.player.distanceTo(p), range.value) & 0xE0FFFFFF;
            Overlay3D.tracer(ctx, v, chest, col);
        }
    }
}
