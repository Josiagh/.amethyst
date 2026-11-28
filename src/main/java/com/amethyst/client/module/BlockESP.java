package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Highlights chosen ores and blocks through walls. Only sees chunks your client has loaded. */
public class BlockESP extends Module {
    public final NumSetting range = new NumSetting("Range", 3, 1, 8);
    public final NumSetting maxBoxes = new NumSetting("Max Boxes", 300, 50, 1000);
    public final BoolSetting diamonds = new BoolSetting("Diamonds", true);
    public final BoolSetting debris = new BoolSetting("Ancient Debris", true);
    public final BoolSetting spawners = new BoolSetting("Spawners", true);
    public final BoolSetting emeralds = new BoolSetting("Emeralds", false);
    public final BoolSetting gold = new BoolSetting("Gold", false);

    private record Hit(BlockPos pos, int color) {}
    private volatile List<Hit> hits = List.of();
    private int timer = 999;

    public BlockESP() {
        super("BlockESP", Category.RENDER);
        nums.add(range);
        nums.add(maxBoxes);
        bools.add(diamonds);
        bools.add(debris);
        bools.add(spawners);
        bools.add(emeralds);
        bools.add(gold);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (isEnabled()) draw(ctx, tc.getTickProgress(true));
        });
    }

    @Override protected void onEnable() { timer = 999; }
    @Override protected void onDisable() { hits = List.of(); }

    private int colorOf(Block b) {
        if (diamonds.value && (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE)) return 0xFF55FFFF;
        if (debris.value && b == Blocks.ANCIENT_DEBRIS) return 0xFFB5651D;
        if (spawners.value && b == Blocks.SPAWNER) return 0xFFFF3030;
        if (emeralds.value && (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE)) return 0xFF40FF60;
        if (gold.value && (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE)) return 0xFFFFD740;
        return 0;
    }

    @Override public void onTick() {
        if (++timer < 40) return;
        timer = 0;

        List<Hit> out = new ArrayList<>();
        ChunkPos c = mc.player.getChunkPos();
        int r = (int) range.value;
        int cap = (int) maxBoxes.value * 3;

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                Chunk ch = mc.world.getChunk(c.x + dx, c.z + dz, ChunkStatus.FULL, false);
                if (!(ch instanceof WorldChunk wc)) continue;
                ChunkSection[] secs = wc.getSectionArray();
                int bottom = wc.getBottomSectionCoord();
                for (int i = 0; i < secs.length; i++) {
                    ChunkSection s = secs[i];
                    if (s == null || s.isEmpty()) continue;
                    if (!s.hasAny(st -> colorOf(st.getBlock()) != 0)) continue;
                    int baseY = (bottom + i) * 16;
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                int col = colorOf(s.getBlockState(x, y, z).getBlock());
                                if (col != 0 && out.size() < cap)
                                    out.add(new Hit(new BlockPos(wc.getPos().getStartX() + x, baseY + y, wc.getPos().getStartZ() + z), col));
                            }
                        }
                    }
                }
            }
        }

        var me = mc.player.getBlockPos();
        out.sort(Comparator.comparingDouble(h -> h.pos().getSquaredDistance(me)));
        int keep = (int) maxBoxes.value;
        hits = out.size() > keep ? new ArrayList<>(out.subList(0, keep)) : out;
    }

    private void draw(DrawContext ctx, float td) {
        if (mc.player == null || mc.world == null) return;
        List<Hit> list = hits;
        if (list.isEmpty()) return;
        Overlay3D.View v = Overlay3D.view(td);
        for (Hit h : list) Overlay3D.box(ctx, v, new Box(h.pos()), h.color());
    }
}
