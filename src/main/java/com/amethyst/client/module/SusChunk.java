package com.amethyst.client.module;

import net.minecraft.block.entity.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Passive: scores already-loaded chunks by player-built block entities. Sends no packets. */
public class SusChunk extends Module {
    public final NumSetting threshold = new NumSetting("Threshold", 8, 2, 60);
    public final NumSetting radius = new NumSetting("Radius", 12, 2, 32);
    public final BoolSetting chatAlerts = new BoolSetting("Chat Alerts", true);

    public static final Map<ChunkPos, Double> results = new ConcurrentHashMap<>();
    private final Set<ChunkPos> alerted = new HashSet<>();
    private int timer;

    public SusChunk() {
        super("SusChunk", Category.WORLD);
        nums.add(threshold);
        nums.add(radius);
        bools.add(chatAlerts);
    }

    @Override protected void onDisable() { results.clear(); alerted.clear(); }

    @Override public void onTick() {
        if (++timer < 40) return; // rescan every 2 seconds
        timer = 0;

        ChunkPos center = mc.player.getChunkPos();
        int r = (int) radius.value;

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                Chunk c = mc.world.getChunk(center.x + dx, center.z + dz, ChunkStatus.FULL, false);
                if (!(c instanceof WorldChunk chunk)) continue;

                double score = score(chunk);
                ChunkPos pos = chunk.getPos();
                if (score >= threshold.value) {
                    results.put(pos, score);
                    if (chatAlerts.value && alerted.add(pos)) {
                        mc.player.sendMessage(Text.literal(String.format(
                            "[SusChunk] score %.1f at %d, %d", score, pos.getStartX(), pos.getStartZ())), false);
                    }
                } else {
                    results.remove(pos);
                }
            }
        }
    }

    private double score(WorldChunk chunk) {
        double s = 0;
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof ShulkerBoxBlockEntity) s += 3;
            else if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity
                  || be instanceof EnderChestBlockEntity) s += 1;
            else if (be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity
                  || be instanceof FurnaceBlockEntity) s += 0.5;
            else if (be instanceof BedBlockEntity || be instanceof SignBlockEntity) s += 0.25;
        }
        return s;
    }

    public static List<Map.Entry<ChunkPos, Double>> top(int n) {
        return results.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(n).toList();
    }
}
