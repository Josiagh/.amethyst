package com.amethyst.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class Overlay3D {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static final class View {
        public Vec3d cam, f, r, u;
        public double tan, aspect;
        public int w, h;
    }

    public static View view(float td) {
        View v = new View();
        float yaw, pitch;
        if (ModuleManager.FREECAM.isEnabled()) {
            v.cam = new Vec3d(MathHelper.lerp(td, CameraState.prevX, CameraState.x),
                              MathHelper.lerp(td, CameraState.prevY, CameraState.y),
                              MathHelper.lerp(td, CameraState.prevZ, CameraState.z));
            yaw = CameraState.yaw;
            pitch = CameraState.pitch;
        } else if (ModuleManager.FREELOOK.isEnabled()) {
            yaw = CameraState.yaw;
            pitch = CameraState.pitch;
            double yr0 = Math.toRadians(yaw), pr0 = Math.toRadians(pitch);
            Vec3d eye = mc.player.getLerpedPos(td).add(0, mc.player.getStandingEyeHeight(), 0);
            v.cam = eye.subtract(-Math.sin(yr0) * Math.cos(pr0) * 4, -Math.sin(pr0) * 4, Math.cos(yr0) * Math.cos(pr0) * 4);
        } else {
            v.cam = mc.player.getLerpedPos(td).add(0, mc.player.getStandingEyeHeight(), 0);
            yaw = mc.player.getYaw(td);
            pitch = mc.player.getPitch(td);
        }
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        v.f = new Vec3d(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
        v.r = new Vec3d(-Math.cos(yr), 0, -Math.sin(yr));
        v.u = v.r.crossProduct(v.f);
        v.tan = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        v.w = mc.getWindow().getScaledWidth();
        v.h = mc.getWindow().getScaledHeight();
        v.aspect = (double) v.w / v.h;
        return v;
    }

    /** out = {screenX, screenY, depth}. Returns false if the point is behind the camera. */
    public static boolean project(View v, Vec3d p, double[] out) {
        Vec3d d = p.subtract(v.cam);
        double z = d.dotProduct(v.f);
        if (z < 0.05) return false;
        out[0] = (d.dotProduct(v.r) / (z * v.tan * v.aspect) * 0.5 + 0.5) * v.w;
        out[1] = (0.5 - d.dotProduct(v.u) / (z * v.tan) * 0.5) * v.h;
        out[2] = z;
        return true;
    }

    public static void tracer(DrawContext ctx, View v, Vec3d target, int col) {
        double[] o = new double[3];
        if (project(v, target, o)) line(ctx, v.w / 2.0, v.h / 2.0, o[0], o[1], col, v.w, v.h);
    }

    public static void box(DrawContext ctx, View v, Box b, int col) {
        double[][] c = new double[8][];
        for (int i = 0; i < 8; i++) {
            Vec3d d = new Vec3d((i & 1) == 0 ? b.minX : b.maxX,
                                (i & 2) == 0 ? b.minY : b.maxY,
                                (i & 4) == 0 ? b.minZ : b.maxZ).subtract(v.cam);
            c[i] = new double[]{d.dotProduct(v.r), d.dotProduct(v.u), d.dotProduct(v.f)};
        }
        for (int i = 0; i < 8; i++) {
            for (int bit = 1; bit <= 4; bit <<= 1) {
                if ((i & bit) == 0) edge(ctx, v, c[i], c[i | bit], col);
            }
        }
    }

    private static void edge(DrawContext ctx, View v, double[] a, double[] b, int col) {
        double near = 0.05;
        if (a[2] < near && b[2] < near) return;
        double[] p = a, q = b;
        if (a[2] < near) {
            double k = (near - a[2]) / (b[2] - a[2]);
            p = new double[]{a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k, near};
        } else if (b[2] < near) {
            double k = (near - b[2]) / (a[2] - b[2]);
            q = new double[]{b[0] + (a[0] - b[0]) * k, b[1] + (a[1] - b[1]) * k, near};
        }
        double x0 = (p[0] / (p[2] * v.tan * v.aspect) * 0.5 + 0.5) * v.w;
        double y0 = (0.5 - p[1] / (p[2] * v.tan) * 0.5) * v.h;
        double x1 = (q[0] / (q[2] * v.tan * v.aspect) * 0.5 + 0.5) * v.w;
        double y1 = (0.5 - q[1] / (q[2] * v.tan) * 0.5) * v.h;
        line(ctx, x0, y0, x1, y1, col, v.w, v.h);
    }

    /** 2px line clipped to the screen. */
    public static void line(DrawContext ctx, double x0, double y0, double x1, double y1, int col, int w, int h) {
        double xmin = -20, xmax = w + 20, ymin = -20, ymax = h + 20;
        double dx = x1 - x0, dy = y1 - y0, t0 = 0, t1 = 1;
        double[] pp = {-dx, dx, -dy, dy};
        double[] qq = {x0 - xmin, xmax - x0, y0 - ymin, ymax - y0};
        for (int i = 0; i < 4; i++) {
            if (pp[i] == 0) {
                if (qq[i] < 0) return;
            } else {
                double rr = qq[i] / pp[i];
                if (pp[i] < 0) { if (rr > t1) return; if (rr > t0) t0 = rr; }
                else           { if (rr < t0) return; if (rr < t1) t1 = rr; }
            }
        }
        double ax = x0 + t0 * dx, ay = y0 + t0 * dy, bx = x0 + t1 * dx, by = y0 + t1 * dy;
        if (Math.abs(by - ay) < 1) {
            ctx.fill((int) Math.min(ax, bx), (int) ay, (int) Math.max(ax, bx) + 2, (int) ay + 2, col);
            return;
        }
        if (Math.abs(bx - ax) < 1) {
            ctx.fill((int) ax, (int) Math.min(ay, by), (int) ax + 2, (int) Math.max(ay, by) + 2, col);
            return;
        }
        int steps = (int) Math.ceil(Math.max(Math.abs(bx - ax), Math.abs(by - ay)));
        for (int i = 0; i <= steps; i++) {
            double k = (double) i / steps;
            int x = (int) (ax + (bx - ax) * k);
            int y = (int) (ay + (by - ay) * k);
            ctx.fill(x, y, x + 2, y + 2, col);
        }
    }
}
