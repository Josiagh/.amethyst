package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds player-dug passages: perfectly straight 1x1 vertical shafts and enclosed 1x2 tunnels.
 * Natural caves almost never produce these, so they point at a player's digging.
 */
public class TunnelESP extends Module {
    public final NumSetting range = new NumSetting("Range", 4, 1, 8);
    public final NumSetting minY = new NumSetting("Min Y", -64, -64, 320);
    public final NumSetting maxY = new NumSetting("Max Y", 64, -64, 320);
    public final NumSetting minShaft = new NumSetting("Min Shaft", 6, 3, 30);
    public final NumSetting minTunnel = new NumSetting("Min Tunnel", 10, 6, 16);
    public final BoolSetting shafts = new BoolSetting("Shafts", true);
    public final BoolSetting tunnels = new BoolSetting("Tunnels", true);
    public final NumSetting maxBoxes = new NumSetting("Max Boxes", 200, 20, 600);

    private record Found(Box box, int color) {}
    private final Map<ChunkPos, List<Found>> perChunk = new ConcurrentHashMap<>();
    private volatile List<Found> snapshot = List.of();
    private List<ChunkPos> queue = new ArrayList<>();
    private int cursor;
    private Object lastWorld;

    public TunnelESP() {
        super("TunnelESP", Category.WORLD);
        nums.add(range);
        nums.add(minY);
        nums.add(maxY);
        nums.add(minShaft);
        nums.add(minTunnel);
        nums.add(maxBoxes);
        bools.add(shafts);
        bools.add(tunnels);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (isEnabled()) draw(ctx, tc.getTickProgress(true));
        });
    }

    @Override protected void onEnable() { queue = new ArrayList<>(); cursor = 0; }
    @Override protected void onDisable() { perChunk.clear(); snapshot = List.of(); }

    @Override public void onTick() {
        if (mc.world != lastWorld) {
            lastWorld = mc.world;
            perChunk.clear();
            snapshot = List.of();
            queue = new ArrayList<>();
            cursor = 0;
        }
        if (cursor >= queue.size()) {
            rebuildSnapshot();
            rebuildQueue();
        }

        // one chunk per tick keeps this light
        if (cursor < queue.size()) {
            ChunkPos pos = queue.get(cursor++);
            Chunk ch = mc.world.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
            if (ch instanceof WorldChunk wc) perChunk.put(pos, scan(wc));
        }
    }

    private void rebuildQueue() {
        ChunkPos c = mc.player.getChunkPos();
        int r = (int) range.value;
        List<ChunkPos> list = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                list.add(new ChunkPos(c.x + dx, c.z + dz));
        list.sort(Comparator.comparingInt(p -> (p.x - c.x) * (p.x - c.x) + (p.z - c.z) * (p.z - c.z)));
        queue = list;
        cursor = 0;
        Set<ChunkPos> keep = new HashSet<>(list);
        perChunk.keySet().removeIf(k -> !keep.contains(k));
    }

    private void rebuildSnapshot() {
        List<Found> all = new ArrayList<>();
        for (List<Found> l : perChunk.values()) all.addAll(l);
        Vec3dHolder me = new Vec3dHolder(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        all.sort(Comparator.comparingDouble(f -> f.box().getCenter().squaredDistanceTo(me.x, me.y, me.z)));
        int cap = (int) maxBoxes.value;
        snapshot = all.size() > cap ? new ArrayList<>(all.subList(0, cap)) : all;
    }

    private record Vec3dHolder(double x, double y, double z) {}

    private List<Found> scan(WorldChunk wc) {
        List<Found> out = new ArrayList<>();
        int lo = Math.max((int) minY.value, wc.getBottomY());
        int hi = Math.min((int) maxY.value, wc.getBottomY() + wc.getHeight() - 1);
        int H = hi - lo + 1;
        if (H < 4) return out;

        int sx = wc.getPos().getStartX(), sz = wc.getPos().getStartZ();
        boolean[][][] air = new boolean[16][H][16];
        BlockPos.Mutable mp = new BlockPos.Mutable();
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                for (int y = 0; y < H; y++)
                    air[x][y][z] = wc.getBlockState(mp.set(sx + x, lo + y, sz + z)).isAir();

        if (shafts.value) {
            int min = (int) minShaft.value;
            for (int x = 1; x < 15; x++) {
                for (int z = 1; z < 15; z++) {
                    int run = 0;
                    for (int y = 0; y <= H; y++) {
                        boolean cond = y < H && air[x][y][z]
                            && !air[x - 1][y][z] && !air[x + 1][y][z]
                            && !air[x][y][z - 1] && !air[x][y][z + 1];
                        if (cond) { run++; continue; }
                        if (run >= min)
                            out.add(new Found(new Box(sx + x, lo + y - run, sz + z, sx + x + 1, lo + y, sz + z + 1), 0xFF40E0FF));
                        run = 0;
                    }
                }
            }
        }

        if (tunnels.value) {
            int min = (int) minTunnel.value;
            // tunnels running along X
            for (int z = 1; z < 15; z++) {
                for (int y = 1; y < H - 2; y++) {
                    int run = 0;
                    for (int x = 0; x <= 16; x++) {
                        boolean cond = x < 16 && air[x][y][z] && air[x][y + 1][z]
                            && !air[x][y + 2][z] && !air[x][y - 1][z]
                            && !air[x][y][z - 1] && !air[x][y][z + 1]
                            && !air[x][y + 1][z - 1] && !air[x][y + 1][z + 1];
                        if (cond) { run++; continue; }
                        if (run >= min)
                            out.add(new Found(new Box(sx + x - run, lo + y, sz + z, sx + x, lo + y + 2, sz + z + 1), 0xFFFFE040));
                        run = 0;
                    }
                }
            }
            // tunnels running along Z
            for (int x = 1; x < 15; x++) {
                for (int y = 1; y < H - 2; y++) {
                    int run = 0;
                    for (int z = 0; z <= 16; z++) {
                        boolean cond = z < 16 && air[x][y][z] && air[x][y + 1][z]
                            && !air[x][y + 2][z] && !air[x][y - 1][z]
                            && !air[x - 1][y][z] && !air[x + 1][y][z]
                            && !air[x - 1][y + 1][z] && !air[x + 1][y + 1][z];
                        if (cond) { run++; continue; }
                        if (run >= min)
                            out.add(new Found(new Box(sx + x, lo + y, sz + z - run, sx + x + 1, lo + y + 2, sz + z), 0xFFFFE040));
                        run = 0;
                    }
                }
            }
        }
        return out;
    }

    private void draw(DrawContext ctx, float td) {
        if (mc.player == null || mc.world == null) return;
        List<Found> list = snapshot;
        if (list.isEmpty()) return;
        Overlay3D.View v = Overlay3D.view(td);
        for (Found f : list) Overlay3D.box(ctx, v, f.box(), f.color());
    }
}
