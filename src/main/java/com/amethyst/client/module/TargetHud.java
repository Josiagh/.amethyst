package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/** Shows health and distance of the closest player. */
public class TargetHud extends Module {
    public final NumSetting range = new NumSetting("Range", 12, 3, 64);

    public TargetHud() {
        super("TargetHud", Category.RENDER);
        nums.add(range);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (isEnabled()) draw(ctx);
        });
    }

    private void draw(DrawContext ctx) {
        if (mc.player == null || mc.world == null) return;
        PlayerEntity target = null;
        double best = Double.MAX_VALUE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double d = mc.player.distanceTo(p);
            if (d <= range.value && d < best) { best = d; target = p; }
        }
        if (target == null) return;

        int w = 130, h = 36;
        int x = mc.getWindow().getScaledWidth() / 2 + 24;
        int y = mc.getWindow().getScaledHeight() / 2 + 24;

        float hp = target.getHealth() + target.getAbsorptionAmount();
        float frac = MathHelper.clamp(hp / target.getMaxHealth(), 0f, 1f);
        int bar = frac > 0.6f ? 0xFF55DD55 : frac > 0.3f ? 0xFFFFD54F : 0xFFFF5555;

        ctx.fill(x, y, x + w, y + h, 0xB0000000);
        ctx.fill(x, y, x + w, y + 1, 0xFF8E24AA);
        ctx.drawTextWithShadow(mc.textRenderer, target.getName().getString(), x + 5, y + 5, 0xFFFFFFFF);
        ctx.drawTextWithShadow(mc.textRenderer, String.format("%d hp  %dm", Math.round(hp), (int) best), x + 5, y + 16, 0xFFCCCCCC);
        ctx.fill(x + 5, y + 28, x + w - 5, y + 31, 0xFF303030);
        ctx.fill(x + 5, y + 28, x + 5 + (int) ((w - 10) * frac), y + 31, bar);
    }
}
