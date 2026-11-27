package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.entity.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

public class BaseESP extends Module {
    public final NumSetting range = new NumSetting("Range", 6, 1, 16);
    public final BoolSetting chests = new BoolSetting("Chests", true);
    public final BoolSetting enderChests = new BoolSetting("Ender Chests", true);
    public final BoolSetting shulkers = new BoolSetting("Shulkers", true);
    public final BoolSetting hoppers = new BoolSetting("Hoppers", true);
    public final BoolSetting droppers = new BoolSetting("Droppers", true);
    public final BoolSetting dispensers = new BoolSetting("Dispensers", true);

    private record Esp(BlockPos pos, int color) {}
    private volatile List<Esp> targets = List.of();
    private int timer = 999;

    public BaseESP() {
        super("BaseESP", Category.RENDER);
        nums.add(range);
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
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                Chunk ch = mc.world.getChunk(c.x + dx, c.z + dz, ChunkStatus.FULL, false);
                if (!(ch instanceof WorldChunk wc)) continue;
                for (BlockEntity be : wc.getBlockEntities().values()) {
                    int col = colorFor(be);
                    if (col != 0) out.add(new Esp(be.getPos(), col));
                }
            }
        }
        targets = out;
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

        double fov = mc.options.getFov().getValue();
        double tan = Math.tan(Math.toRadians(fov) / 2.0);
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        double aspect = (double) w / h;

        for (Esp t : list) {
            BlockPos p = t.pos();
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            boolean ok = true;

            for (int i = 0; i < 8; i++) {
                Vec3d d = new Vec3d(p.getX() + (i & 1), p.getY() + ((i >> 1) & 1), p.getZ() + ((i >> 2) & 1)).subtract(cam);
                double z = d.dotProduct(f);
                if (z < 0.05) { ok = false; break; }
                double sx = (d.dotProduct(r) / (z * tan * aspect) * 0.5 + 0.5) * w;
                double sy = (0.5 - d.dotProduct(u) / (z * tan) * 0.5) * h;
                minX = Math.min(minX, sx); maxX = Math.max(maxX, sx);
                minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
            }
            if (!ok) continue;
            if (maxX < 0 || minX > w || maxY < 0 || minY > h) continue;

            int x1 = (int) MathHelper.clamp(minX, -5, w + 5);
            int x2 = (int) MathHelper.clamp(maxX, -5, w + 5);
            int y1 = (int) MathHelper.clamp(minY, -5, h + 5);
            int y2 = (int) MathHelper.clamp(maxY, -5, h + 5);
            if (x2 - x1 < 2) x2 = x1 + 2;
            if (y2 - y1 < 2) y2 = y1 + 2;

            int col = t.color();
            ctx.fill(x1, y1, x2, y2, (col & 0x00FFFFFF) | 0x30000000);
            ctx.fill(x1, y1, x2, y1 + 1, col);
            ctx.fill(x1, y2 - 1, x2, y2, col);
            ctx.fill(x1, y1, x1 + 1, y2, col);
            ctx.fill(x2 - 1, y1, x2, y2, col);
        }
    }
}
