package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Stick-figure outline. The pose is approximate: it follows where the player faces, not limb animation. */
public class SkeletonESP extends Module {
    public final NumSetting range = new NumSetting("Range", 96, 16, 512);

    public SkeletonESP() {
        super("SkeletonESP", Category.RENDER);
        nums.add(range);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (isEnabled()) draw(ctx, tc.getTickProgress(true));
        });
    }

    private void draw(DrawContext ctx, float td) {
        if (mc.player == null || mc.world == null) return;
        Overlay3D.View v = Overlay3D.view(td);
        for (PlayerEntity p : RenderUtil.players(range.value)) {
            Vec3d base = RenderUtil.pos(p, td);
            double yr = Math.toRadians(p.getYaw(td));
            Vec3d right = new Vec3d(-Math.cos(yr), 0, -Math.sin(yr));
            double s = p.getHeight() / 1.8;
            int col = RenderUtil.distColor(mc.player.distanceTo(p), range.value);

            Vec3d head = base.add(0, 1.65 * s, 0);
            Vec3d neck = base.add(0, 1.45 * s, 0);
            Vec3d hip = base.add(0, 0.75 * s, 0);
            Vec3d lSh = neck.subtract(right.multiply(0.3));
            Vec3d rSh = neck.add(right.multiply(0.3));
            Vec3d lHand = lSh.add(0, -0.6 * s, 0);
            Vec3d rHand = rSh.add(0, -0.6 * s, 0);
            Vec3d lHip = hip.subtract(right.multiply(0.12));
            Vec3d rHip = hip.add(right.multiply(0.12));
            Vec3d lFoot = base.subtract(right.multiply(0.12));
            Vec3d rFoot = base.add(right.multiply(0.12));

            bone(ctx, v, head, neck, col);
            bone(ctx, v, neck, hip, col);
            bone(ctx, v, lSh, rSh, col);
            bone(ctx, v, lSh, lHand, col);
            bone(ctx, v, rSh, rHand, col);
            bone(ctx, v, lHip, rHip, col);
            bone(ctx, v, lHip, lFoot, col);
            bone(ctx, v, rHip, rFoot, col);
        }
    }

    private static void bone(DrawContext ctx, Overlay3D.View v, Vec3d a, Vec3d b, int col) {
        double[] p = new double[3], q = new double[3];
        if (!Overlay3D.project(v, a, p) || !Overlay3D.project(v, b, q)) return;
        Overlay3D.line(ctx, p[0], p[1], q[0], q[1], col, v.w, v.h);
    }
}
