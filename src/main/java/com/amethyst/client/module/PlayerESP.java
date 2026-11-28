package com.amethyst.client.module;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerESP extends Module {
    public final NumSetting range = new NumSetting("Range", 128, 16, 512);
    public final BoolSetting colorByDistance = new BoolSetting("Color By Distance", true);

    public PlayerESP() {
        super("PlayerESP", Category.RENDER);
        nums.add(range);
        bools.add(colorByDistance);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (isEnabled()) draw(ctx, tc.getTickProgress(true));
        });
    }

    private void draw(DrawContext ctx, float td) {
        if (mc.player == null || mc.world == null) return;
        Overlay3D.View v = Overlay3D.view(td);
        for (PlayerEntity p : RenderUtil.players(range.value)) {
            int col = colorByDistance.value
                ? RenderUtil.distColor(mc.player.distanceTo(p), range.value)
                : 0xFFFF4040;
            Overlay3D.box(ctx, v, RenderUtil.box(p, td), col);
        }
    }
}
