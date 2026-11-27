package com.amethyst.client.module;

import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {
    public final NumSetting health = new NumSetting("Health", 20, 1, 20);
    public final NumSetting delay = new NumSetting("Delay", 1, 0, 10);
    private int cooldown;

    public AutoTotem() {
        super("AutoTotem", Category.COMBAT);
        nums.add(health);
        nums.add(delay);
    }

    @Override public void onTick() {
        if (cooldown > 0) { cooldown--; return; }
        if (mc.currentScreen != null) return;
        if (mc.player.getHealth() > health.value) return;
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;

        int slot = findTotem();
        if (slot == -1) return;

        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
        cooldown = (int) delay.value;
    }

    private int findTotem() {
        var inv = mc.player.getInventory();
        for (int i = 9; i < 36; i++) if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        for (int i = 0; i < 9; i++) if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return 36 + i;
        return -1;
    }
}
