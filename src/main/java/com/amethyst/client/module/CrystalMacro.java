package com.amethyst.client.module;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class CrystalMacro extends Module {
    public static CrystalMacro INSTANCE;

    public final NumSetting placeDelay = new NumSetting("Place Delay", 2, 0, 10);
    public final NumSetting breakDelay = new NumSetting("Break Delay", 1, 0, 10);
    private int cooldown;

    public CrystalMacro() {
        super("CrystalMacro", Category.COMBAT);
        nums.add(placeDelay);
        nums.add(breakDelay);
        INSTANCE = this;
    }

    /** Stops vanilla right-click from also firing while this macro handles crystals. */
    public static boolean cancelUse() {
        return INSTANCE != null && INSTANCE.isEnabled() && mc.player != null
            && mc.currentScreen == null
            && mc.player.getMainHandStack().isOf(Items.END_CRYSTAL);
    }

    @Override public void onTick() {
        if (cooldown > 0) cooldown--;
        if (mc.currentScreen != null || !mc.options.useKey.isPressed()) return;
        if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) return;
        if (cooldown > 0) return;

        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystalEntity crystal) {
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            cooldown = (int) breakDelay.value;
        } else if (hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            BlockState state = mc.world.getBlockState(bhr.getBlockPos());
            boolean base = state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
            if (base && mc.world.isAir(bhr.getBlockPos().up())) {
                if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr).isAccepted())
                    mc.player.swingHand(Hand.MAIN_HAND);
                cooldown = (int) placeDelay.value;
            }
        }
    }
}
