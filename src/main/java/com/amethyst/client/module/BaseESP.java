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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BaseESP extends Module {
    public final NumSetting range = new NumSetting("Range", 16, 1, 32);
    public final NumSetting maxDistance = new NumSetting("Max Distance", 512, 16, 1024);
    public final NumSetting maxBoxes = new NumSetting("Max Boxes", 600, 50, 1500);
    public final NumSetting minY = new NumSetting("Min Y", -64, -64, 320);
    public final NumSetting maxY = new NumSetting("Max Y", 320, -64, 320);
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
        nums.add(maxBoxes);
        nums.add(minY);
        nums.add(maxY);
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
        if (++timer < 20) return;
        timer = 0;

        List<Esp> out = new ArrayList<>();
        ChunkPos c = mc.player.getChunkPos();
        int r = (int) range.value;
        double maxSq = maxDistance.value * maxDistance.value;
        int lo = (int) minY.value, hi = (int) maxY.value;

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                Chunk ch = mc.world.getChunk(c.x + dx, c.z + dz, ChunkStatus.FULL, false);
                if (!(ch instanceof WorldChunk wc)) continue;
                for (BlockEntity be : wc.getBlockEntities().values()) {
                    BlockPos p = be.getPos();
                    if (p.getY() < lo || p.getY() > hi) continue;
                    int col = colorFor(be);
                    if (col == 0) continue;
                    if (mc.player.squaredDistanceTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) > maxSq) continue;

                    BlockState st = be.getCachedState();
                    VoxelShape shape = st.getOutlineShape(mc.world, p);
                    Box box = shape.isEmpty() ? new Box(p) : shape.getBoundingBox().offset(p);

                    if (st.getBlock() instanceof ChestBlock && st.contains(ChestBlock.CHEST_TYPE)) {
                        ChestType type = st.get(ChestBlock.CHEST_TYPE);
                        if (type == ChestType.LEFT) continue;
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
        int cap = (int) maxBoxes.value;
        targets = out.size() > cap ? new ArrayList<>(out.subList(0, cap)) : out;
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

        Overlay3D.View v = Overlay3D.view(td);
        for (Esp t : list) {
            Overlay3D.box(ctx, v, t.box(), t.color());
            if (tracers.value) Overlay3D.tracer(ctx, v, t.box().getCenter(), t.color() & 0xB0FFFFFF);
        }
    }
}
