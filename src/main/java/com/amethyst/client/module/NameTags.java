package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class NameTags extends Module {
    public final NumSetting range = new NumSetting("Range", 256, 16, 1024);
    public final BoolSetting health = new BoolSetting("Health", true);
    public final BoolSetting distance = new BoolSetting("Distance", true);

    public NameTags() {
        super("NameTags", Category.RENDER);
        nums.add(range);
        bools.add(health);
        bools.add(distance);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (isEnabled()) draw(ctx, tc.getTickProgress(true));
        });
    }

    private void draw(DrawContext ctx, float td) {
        if (mc.player == null || mc.world == null) return;
        Overlay3D.View v = Overlay3D.view(td);
        double[] o = new double[3];
        for (PlayerEntity p : RenderUtil.players(range.value)) {
            if (p == mc.player) continue;
            Vec3d top = RenderUtil.pos(p, td).add(0, p.getHeight() + 0.55, 0);
            if (!Overlay3D.project(v, top, o)) continue;
            if (o[0] < -50 || o[0] > v.w + 50 || o[1] < -20 || o[1] > v.h + 20) continue;

            StringBuilder sb = new StringBuilder(p.getName().getString());
            int color = 0xFFFFFFFF;
            if (health.value) {
                int hp = Math.round(p.getHealth() + p.getAbsorptionAmount());
                sb.append("  ").append(hp);
                color = hp > 14 ? 0xFF80FF80 : hp > 7 ? 0xFFFFD54F : 0xFFFF6060;
            }
            if (distance.value) sb.append("  ").append((int) mc.player.distanceTo(p)).append("m");
            RenderUtil.text(ctx, sb.toString(), (int) o[0], (int) o[1], color);
        }
    }
}
