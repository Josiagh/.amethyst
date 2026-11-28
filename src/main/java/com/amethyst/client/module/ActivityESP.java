package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Finds chunks with lots of entities: item frames, armor stands, storage minecarts, other players, mob farms. */
public class ActivityESP extends Module {
    public final NumSetting threshold = new NumSetting("Threshold", 6, 2, 40);
    public final NumSetting count = new NumSetting("ESP Count", 8, 1, 20);
    public final BoolSetting tracers = new BoolSetting("Tracers", false);
    public final BoolSetting hudList = new BoolSetting("HUD List", true);

    private record Spot(ChunkPos pos, double score, double y) {}
    private volatile List<Spot> spots = List.of();
    private int timer = 999;

    public ActivityESP() {
        super("ActivityESP", Category.WORLD);
        nums.add(threshold);
        nums.add(count);
        bools.add(tracers);
        bools.add(hudList);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (isEnabled()) draw(ctx, tc.getTickProgress(true));
        });
    }

    @Override protected void onEnable() { timer = 999; }
    @Override protected void onDisable() { spots = List.of(); }

    private static double weight(Entity e, Entity self) {
        EntityType<?> t = e.getType();
        if (e instanceof PlayerEntity) return e == self ? 0 : 4;
        if (t == EntityType.ITEM_FRAME || t == EntityType.GLOW_ITEM_FRAME) return 1.0;
        if (t == EntityType.ARMOR_STAND) return 1.5;
        if (t == EntityType.CHEST_MINECART) return 2.0;
        if (t == EntityType.HOPPER_MINECART) return 1.0;
        if (t == EntityType.VILLAGER) return 0.6;
        if (t == EntityType.ITEM) return 0.05;
        if (e instanceof MobEntity) return 0.15;
        return 0;
    }

    @Override public void onTick() {
        if (++timer < 20) return;
        timer = 0;

        Map<ChunkPos, double[]> map = new HashMap<>();
        for (Entity e : mc.world.getEntities()) {
            double w = weight(e, mc.player);
            if (w <= 0) continue;
            double[] a = map.computeIfAbsent(e.getChunkPos(), k -> new double[3]);
            a[0] += w;
            a[1] += e.getY() * w;
            a[2] += w;
        }

        List<Spot> out = new ArrayList<>();
        for (Map.Entry<ChunkPos, double[]> en : map.entrySet()) {
            double[] a = en.getValue();
            if (a[0] >= threshold.value) out.add(new Spot(en.getKey(), a[0], a[1] / a[2]));
        }
        out.sort((x, y) -> Double.compare(y.score(), x.score()));
        int cap = (int) count.value;
        spots = out.size() > cap ? new ArrayList<>(out.subList(0, cap)) : out;
    }

    private void draw(DrawContext ctx, float td) {
        if (mc.player == null || mc.world == null) return;
        List<Spot> list = spots;
        if (list.isEmpty()) return;
        Overlay3D.View v = Overlay3D.view(td);
        double[] o = new double[3];

        for (Spot s : list) {
            ChunkPos p = s.pos();
            Box b = new Box(p.getStartX(), s.y() - 3, p.getStartZ(), p.getStartX() + 16, s.y() + 5, p.getStartZ() + 16);
            double ratio = MathHelper.clamp(s.score() / (threshold.value * 4), 0, 1);
            int col = 0xFF000000 | (255 << 16) | ((int) (230 * (1 - ratio)) << 8) | 0x30;
            Overlay3D.box(ctx, v, b, col);

            Vec3d center = b.getCenter();
            if (tracers.value) Overlay3D.tracer(ctx, v, center, col & 0xB0FFFFFF);
            if (Overlay3D.project(v, center, o) && o[0] > 0 && o[0] < v.w && o[1] > 0 && o[1] < v.h) {
                int dist = (int) Math.sqrt(mc.player.squaredDistanceTo(center));
                RenderUtil.text(ctx, String.format("%.0f  %dm", s.score(), dist), (int) o[0], (int) o[1] - 4, 0xFFFFFFFF);
            }
        }

        if (hudList.value) {
            int y = 80;
            for (Spot s : list) {
                if (y > 80 + 11 * 5) break;
                double d = Math.sqrt(mc.player.squaredDistanceTo(s.pos().getStartX() + 8, s.y(), s.pos().getStartZ() + 8));
                ctx.drawTextWithShadow(mc.textRenderer,
                    String.format("ACTIVITY %d, %d  [%.0f]  %dm", s.pos().getStartX() + 8, s.pos().getStartZ() + 8, s.score(), (int) d),
                    6, y, 0xFF80DEEA);
                y += 11;
            }
        }
    }
}
