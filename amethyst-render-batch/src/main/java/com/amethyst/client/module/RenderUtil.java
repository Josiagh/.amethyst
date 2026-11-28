package com.amethyst.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Small helpers shared by the render modules. */
public final class RenderUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /** Players worth drawing: everyone but you, unless Freecam is on (then you show up too). */
    public static List<PlayerEntity> players(double range) {
        List<PlayerEntity> out = new ArrayList<>();
        if (mc.world == null || mc.player == null) return out;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player && !ModuleManager.FREECAM.isEnabled()) continue;
            if (mc.player.distanceTo(p) > range) continue;
            out.add(p);
        }
        return out;
    }

    public static Vec3d pos(PlayerEntity p, float td) {
        return new Vec3d(MathHelper.lerp(td, p.lastX, p.getX()),
                         MathHelper.lerp(td, p.lastY, p.getY()),
                         MathHelper.lerp(td, p.lastZ, p.getZ()));
    }

    public static Box box(PlayerEntity p, float td) {
        Vec3d l = pos(p, td);
        return p.getBoundingBox().offset(l.x - p.getX(), l.y - p.getY(), l.z - p.getZ());
    }

    /** Red when close, green when far. */
    public static int distColor(double d, double max) {
        double t = MathHelper.clamp(d / Math.max(max, 1), 0, 1);
        int r = (int) (255 * (1 - t)), g = (int) (255 * t);
        return 0xFF000000 | (r << 16) | (g << 8) | 0x30;
    }

    public static void text(DrawContext ctx, String s, int cx, int y, int color) {
        int w = mc.textRenderer.getWidth(s);
        ctx.fill(cx - w / 2 - 2, y - 2, cx + w / 2 + 2, y + 10, 0x90000000);
        ctx.drawTextWithShadow(mc.textRenderer, s, cx - w / 2, y, color);
    }
}
