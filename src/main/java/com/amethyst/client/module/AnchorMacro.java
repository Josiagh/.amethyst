package com.amethyst.client.module;

import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AnchorMacro extends Module {
    public static AnchorMacro INSTANCE;

    public final NumSetting charges = new NumSetting("Charges", 1, 1, 4);
    public final NumSetting explodeSlot = new NumSetting("Explode Slot", 1, 1, 9);
    public final NumSetting delay = new NumSetting("Delay", 1, 0, 5);

    private enum Step { IDLE, WAIT_PLACE, CHARGE, EXPLODE }
    private Step step = Step.IDLE;
    private BlockPos anchorPos;
    private int wait, age, clicks;

    public AnchorMacro() {
        super("AnchorMacro", Category.COMBAT);
        nums.add(charges);
        nums.add(explodeSlot);
        nums.add(delay);
        INSTANCE = this;
    }

    public static boolean cancelUse() {
        return INSTANCE != null && INSTANCE.isEnabled() && mc.player != null
            && (INSTANCE.step != Step.IDLE || mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR));
    }

    @Override protected void onDisable() { reset(); }

    private void reset() { step = Step.IDLE; anchorPos = null; clicks = 0; age = 0; }

    private void msg(String s) { mc.player.sendMessage(Text.literal("[Amethyst] " + s), true); }

    private int findHotbar(Item item) {
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    private void click(BlockPos pos) {
        BlockHitResult r = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, r).isAccepted())
            mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Override public void onTick() {
        if (mc.currentScreen != null) return;
        if (step != Step.IDLE && ++age > 100) { reset(); return; }
        if (wait > 0) { wait--; return; }

        switch (step) {
            case IDLE -> {
                if (!mc.options.useKey.isPressed()) return;
                if (!mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) return;
                if (!(mc.crosshairTarget instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return;
                BlockPos target = mc.world.getBlockState(bhr.getBlockPos()).isReplaceable()
                    ? bhr.getBlockPos() : bhr.getBlockPos().offset(bhr.getSide());
                if (!mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr).isAccepted()) return;
                mc.player.swingHand(Hand.MAIN_HAND);
                anchorPos = target;
                step = Step.WAIT_PLACE;
                age = 0;
            }
            case WAIT_PLACE -> {
                if (mc.world.getBlockState(anchorPos).isOf(Blocks.RESPAWN_ANCHOR)) {
                    step = Step.CHARGE;
                    wait = (int) delay.value;
                }
            }
            case CHARGE -> {
                if (!mc.world.getBlockState(anchorPos).isOf(Blocks.RESPAWN_ANCHOR)) { reset(); return; }
                int have = mc.world.getBlockState(anchorPos).get(RespawnAnchorBlock.CHARGES);
                int goal = (int) charges.value;
                if (have >= goal || clicks >= goal) {
                    step = Step.EXPLODE;
                    wait = (int) delay.value;
                    return;
                }
                int slot = findHotbar(Items.GLOWSTONE);
                if (slot == -1) { msg("No glowstone in hotbar"); reset(); return; }
                mc.player.getInventory().setSelectedSlot(slot);
                click(anchorPos);
                clicks++;
                wait = (int) delay.value + 1;
            }
            case EXPLODE -> {
                mc.player.getInventory().setSelectedSlot((int) explodeSlot.value - 1);
                if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                    msg("Explode Slot has glowstone, pick another slot");
                    reset();
                    return;
                }
                click(anchorPos);
                reset();
                wait = 5;
            }
        }
    }
}
