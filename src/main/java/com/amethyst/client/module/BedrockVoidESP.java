package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Holes dug through the bedrock layers: the Nether roof and floor, and the Overworld floor. */
public class BedrockVoidESP extends Module {
    public final NumSetting range = new NumSetting("Range", 5, 1, 8);
    public final BoolSetting tracers = new BoolSetting("Tracers", true);

    private volatile List<Box> holes = List.of();
    private int timer = 999;

    public BedrockVoidESP() {
        super("BedrockVoidESP", Category.WORLD);
        nums.add(range);
        bools.add(tracers);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (isEnabled()) draw(ctx, tc.getTickProgress(true));
        });
    }

    @Override protected void onEnable() { timer = 999; }
    @Override protected void onDisable() { holes = List.of(); }

    private boolean bedrock(BlockPos p) {
        return mc.world.getBlockState(p).isOf(Blocks.BEDROCK);
    }

    private boolean edgeOfBedrock(BlockPos p) {
        int n = 0;
        if (bedrock(p.east())) n++;
        if (bedrock(p.west())) n++;
        if (bedrock(p.north())) n++;
        if (bedrock(p.south())) n++;
        return n >= 2;
    }

    @Override public void onTick() {
        if (++timer < 40) return;
        timer = 0;

        String dim = mc.world.getRegistryKey().getValue().getPath();
        boolean nether = dim.equals("the_nether");
        boolean overworld = dim.equals("overworld");
        if (!nether && !overworld) { holes = List.of(); return; }

        List<Box> out = new ArrayList<>();
        ChunkPos c = mc.player.getChunkPos();
        int r = (int) range.value;
        BlockPos.Mutable mp = new BlockPos.Mutable();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                Chunk ch = mc.world.getChunk(c.x + dx, c.z + dz, ChunkStatus.FULL, false);
                if (!(ch instanceof WorldChunk wc)) continue;
                int sx = wc.getPos().getStartX(), sz = wc.getPos().getStartZ();
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (nether) {
                            check(out, mp.set(sx + x, 127, sz + z).toImmutable());
                            check(out, mp.set(sx + x, 0, sz + z).toImmutable());
                        } else {
                            check(out, mp.set(sx + x, wc.getBottomY(), sz + z).toImmutable());
                        }
                    }
                }
            }
        }

        var me = mc.player.getBlockPos();
        out.sort(Comparator.comparingDouble(b -> b.getCenter().squaredDistanceTo(me.getX(), me.getY(), me.getZ())));
        holes = out.size() > 200 ? new ArrayList<>(out.subList(0, 200)) : out;
    }

    private void check(List<Box> out, BlockPos p) {
        if (mc.world.getBlockState(p).isAir() && edgeOfBedrock(p)) out.add(new Box(p));
    }

    private void draw(DrawContext ctx, float td) {
        if (mc.player == null || mc.world == null) return;
        List<Box> list = holes;
        if (list.isEmpty()) return;
        Overlay3D.View v = Overlay3D.view(td);
        for (Box b : list) {
            Overlay3D.box(ctx, v, b, 0xFFFF55FF);
            if (tracers.value) Overlay3D.tracer(ctx, v, b.getCenter(), 0x90FF55FF);
        }
    }
}
