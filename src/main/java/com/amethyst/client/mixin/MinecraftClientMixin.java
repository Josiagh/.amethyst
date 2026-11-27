package com.amethyst.client.mixin;

import com.amethyst.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    private static boolean amethyst$blocked() {
        return ModuleManager.FREECAM.isEnabled() && ModuleManager.FREECAM.blockInteract.value;
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void amethyst$attack(CallbackInfoReturnable<Boolean> cir) {
        if (amethyst$blocked()) cir.setReturnValue(false);
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void amethyst$use(CallbackInfo ci) {
        if (amethyst$blocked()) ci.cancel();
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void amethyst$breaking(boolean breaking, CallbackInfo ci) {
        if (amethyst$blocked()) ci.cancel();
    }
}
