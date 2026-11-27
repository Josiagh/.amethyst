package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BaseESP extends Module {
    public final NumSetting range = new NumSetting("Range", 6, 1, 16);
    public final NumSetting maxDistance = new NumSetting("Max Distance", 128, 16, 256);
    public final BoolSetting tracers = new BoolSetting("Tracers", false);
    public final BoolSetting chests = new BoolSetting("Chests", true);
    public final BoolSetting enderChests = new BoolSetting("Ender Chests", true);
    public final BoolSetting shulkers = new BoolSetting("Shulkers", true);
    public final BoolSetting hoppers = new BoolSetting("Hoppers", true);
    public final BoolSetting droppers = new BoolSetting("Droppers", true);
    public final BoolSetting dispensers = new BoolSetting("Dispensers", true);

    private record Esp(Box box, int color) {}
    private volatile List<Esp> targets = List.of();
    private int timer = 999;

    public BaseESP() {
        super("BaseESP", Category.RENDER);
        nums.add(range);
        nums.add(maxDistance);
        bools.add(tracers);
        bools.add(chests);
        bools.add(enderChests);
        bools.add(shulkers);
        bools.add(hoppers);
        bools.add(droppers);
        bools.add(dispensers);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (isEnabled()) draw(ctx, tc.getTickProgress(true));
        });
    }

    @Override protected void onEnable() { timer = 999; }
    @Override protected void onDisable() { targets = List.of(); }

    @Override public void onTick() {
        if (++timer < 10) return;
        timer = 0;

        List<Esp> out = new ArrayList<>();
        ChunkPos c = mc.player.getChunkPos();
        int r = (int) range.value;
        double maxSq = maxDistance.value * maxDistance.value;

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                Chunk ch = mc.world.getChunk(c.x + dx, c.z + dz, ChunkStatus.FULL, false);
                if (!(ch instanceof WorldChunk wc)) continue;
                for (BlockEntity be : wc.getBlockEntities().values()) {
                    int col = colorFor(be);
                    if (col == 0) continue;

                    BlockPos p = be.getPos();
                    if (mc.player.squaredDistanceTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) > maxSq) continue;

                    BlockState st = be.getCachedState();
                    VoxelShape shape = st.getOutlineShape(mc.world, p);
                    Box box = shape.isEmpty() ? new Box(p) : shape.getBoundingBox().offset(p);

                    if (st.getBlock() instanceof ChestBlock && st.contains(ChestBlock.CHEST_TYPE)) {
                        ChestType type = st.get(ChestBlock.CHEST_TYPE);
                        if (type == ChestType.LEFT) continue; // the RIGHT half draws the merged box
                        if (type == ChestType.RIGHT) {
                            Direction d = ChestBlock.getFacing(st);
                            box = box.union(box.offset(d.getOffsetX(), d.getOffsetY(), d.getOffsetZ()));
                        }
                    }
                    out.add(new Esp(box, col));
                }
            }
        }

        Vec3d me = mc.player.getEyePos();
        out.sort(Comparator.comparingDouble(e -> e.box().getCenter().squaredDistanceTo(me)));
        targets = out.size() > 300 ? new ArrayList<>(out.subList(0, 300)) : out;
    }

    private int colorFor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return chests.value ? 0xFFFF4040 : 0;
        if (be instanceof ChestBlockEntity) return chests.value ? 0xFFFFAA00 : 0;
        if (be instanceof BarrelBlockEntity) return chests.value ? 0xFFC8842A : 0;
        if (be instanceof EnderChestBlockEntity) return enderChests.value ? 0xFFB040FF : 0;
        if (be instanceof ShulkerBoxBlockEntity) return shulkers.value ? 0xFFFF55FF : 0;
        if (be instanceof HopperBlockEntity) return hoppers.value ? 0xFFDDDDDD : 0;
        if (be instanceof DropperBlockEntity) return droppers.value ? 0xFF55AAFF : 0;
        if (be instanceof DispenserBlockEntity) return dispensers.value ? 0xFF55FF77 : 0;
        return 0;
    }

    private void draw(DrawContext ctx, float td) {
        if (mc.player == null || mc.world == null) return;
        List<Esp> list = targets;
        if (list.isEmpty()) return;

        Vec3d cam;
        float yaw, pitch;
        if (ModuleManager.FREECAM.isEnabled()) {
            cam = new Vec3d(MathHelper.lerp(td, CameraState.prevX, CameraState.x),
                            MathHelper.lerp(td, CameraState.prevY, CameraState.y),
                            MathHelper.lerp(td, CameraState.prevZ, CameraState.z));
            yaw = CameraState.yaw;
            pitch = CameraState.pitch;
        } else if (ModuleManager.FREELOOK.isEnabled()) {
            yaw = CameraState.yaw;
            pitch = CameraState.pitch;
            double yr0 = Math.toRadians(yaw), pr0 = Math.toRadians(pitch);
            Vec3d eye = mc.player.getLerpedPos(td).add(0, mc.player.getStandingEyeHeight(), 0);
            cam = eye.subtract(-Math.sin(yr0) * Math.cos(pr0) * 4, -Math.sin(pr0) * 4, Math.cos(yr0) * Math.cos(pr0) * 4);
        } else {
            cam = mc.player.getLerpedPos(td).add(0, mc.player.getStandingEyeHeight(), 0);
            yaw = mc.player.getYaw(td);
            pitch = mc.player.getPitch(td);
        }

        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        Vec3d f = new Vec3d(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
        Vec3d r = new Vec3d(-Math.cos(yr), 0, -Math.sin(yr));
        Vec3d u = r.crossProduct(f);

        double tan = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        double aspect = (double) w / h;

        for (Esp t : list) {
            Box b = t.box();
            double[][] c = new double[8][];
            for (int i = 0; i < 8; i++) {
                Vec3d d = new Vec3d((i & 1) == 0 ? b.minX : b.maxX,
                                    (i & 2) == 0 ? b.minY : b.maxY,
                                    (i & 4) == 0 ? b.minZ : b.maxZ).subtract(cam);
                c[i] = new double[]{d.dotProduct(r), d.dotProduct(u), d.dotProduct(f)};
            }
            for (int i = 0; i < 8; i++) {
                for (int bit = 1; bit <= 4; bit <<= 1) {
                    if ((i & bit) == 0) edge(ctx, c[i], c[i | bit], t.color(), w, h, tan, aspect);
                }
            }

            if (tracers.value) {
                Vec3d d = b.getCenter().subtract(cam);
                double z = d.dotProduct(f);
                if (z > 0.05) {
                    double sx = (d.dotProduct(r) / (z * tan * aspect) * 0.5 + 0.5) * w;
                    double sy = (0.5 - d.dotProduct(u) / (z * tan) * 0.5) * h;
                    line(ctx, w / 2.0, h / 2.0, sx, sy, t.color() & 0xB0FFFFFF, w, h);
                }
            }
        }
    }

    private static void edge(DrawContext ctx, double[] a, double[] b, int col, int w, int h, double tan, double aspect) {
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
        double x0 = (p[0] / (p[2] * tan * aspect) * 0.5 + 0.5) * w;
        double y0 = (0.5 - p[1] / (p[2] * tan) * 0.5) * h;
        double x1 = (q[0] / (q[2] * tan * aspect) * 0.5 + 0.5) * w;
        double y1 = (0.5 - q[1] / (q[2] * tan) * 0.5) * h;
        line(ctx, x0, y0, x1, y1, col, w, h);
    }

    /** Draws a 2px line, clipped to the screen so off-screen ends cost nothing. */
    private static void line(DrawContext ctx, double x0, double y0, double x1, double y1, int col, int w, int h) {
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
        int steps = (int) Math.ceil(Math.max(Math.abs(bx - ax), Math.abs(by - ay)));
        if (steps < 1) { ctx.fill((int) ax, (int) ay, (int) ax + 2, (int) ay + 2, col); return; }
        for (int i = 0; i <= steps; i++) {
            double k = (double) i / steps;
            int x = (int) (ax + (bx - ax) * k);
            int y = (int) (ay + (by - ay) * k);
            ctx.fill(x, y, x + 2, y + 2, col);
        }
    }
}
