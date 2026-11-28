package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Finds artificial light sources in places that should be dark. Torches in a cave usually mean a player was there. */
public class LightESP extends Module {
    public final NumSetting range = new NumSetting("Range", 3, 1, 8);
    public final NumSetting minY = new NumSetting("Min Y", -64, -64, 320);
    public final NumSetting maxY = new NumSetting("Max Y", 40, -64, 320);
    public final NumSetting maxBoxes = new NumSetting("Max Boxes", 300, 50, 1000);
    public final BoolSetting underground = new BoolSetting("Underground Only", true);
    public final BoolSetting torches = new BoolSetting("Torches", true);
    public final BoolSetting lanterns = new BoolSetting("Lanterns", true);
    public final BoolSetting glow = new BoolSetting("Glowstone Etc", false);

    private record Hit(BlockPos pos, int color) {}
    private volatile List<Hit> hits = List.of();
    private int timer = 999;

    public LightESP() {
        super("LightESP", Category.WORLD);
        nums.add(range);
        nums.add(minY);
        nums.add(maxY);
        nums.add(maxBoxes);
        bools.add(underground);
        bools.add(torches);
        bools.add(lanterns);
        bools.add(glow);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (isEnabled()) draw(ctx, tc.getTickProgress(true));
        });
    }

    @Override protected void onEnable() { timer = 999; }
    @Override protected void onDisable() { hits = List.of(); }

    private int colorOf(Block b) {
        if (torches.value && (b == Blocks.TORCH || b == Blocks.WALL_TORCH
                || b == Blocks.SOUL_TORCH || b == Blocks.SOUL_WALL_TORCH)) return 0xFFFFB300;
        if (lanterns.value && (b == Blocks.LANTERN || b == Blocks.SOUL_LANTERN)) return 0xFF66E0FF;
        if (glow.value && (b == Blocks.GLOWSTONE || b == Blocks.SEA_LANTERN || b == Blocks.SHROOMLIGHT
                || b == Blocks.OCHRE_FROGLIGHT || b == Blocks.VERDANT_FROGLIGHT
                || b == Blocks.PEARLESCENT_FROGLIGHT)) return 0xFFFFF59D;
        return 0;
    }

    @Override public void onTick() {
        if (++timer < 60) return;
        timer = 0;

        List<Hit> out = new ArrayList<>();
        ChunkPos c = mc.player.getChunkPos();
        int r = (int) range.value;
        int lo = (int) minY.value, hi = (int) maxY.value;
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
                    int baseY = (bottom + i) * 16;
                    if (baseY + 15 < lo || baseY > hi) continue;
                    if (!s.hasAny(st -> colorOf(st.getBlock()) != 0)) continue;
                    for (int y = 0; y < 16; y++) {
                        int wy = baseY + y;
                        if (wy < lo || wy > hi) continue;
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                int col = colorOf(s.getBlockState(x, y, z).getBlock());
                                if (col == 0 || out.size() >= cap) continue;
                                BlockPos p = new BlockPos(wc.getPos().getStartX() + x, wy, wc.getPos().getStartZ() + z);
                                if (underground.value && mc.world.getLightLevel(LightType.SKY, p) > 0) continue;
                                out.add(new Hit(p, col));
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
