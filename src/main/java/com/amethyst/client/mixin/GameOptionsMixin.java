package com.amethyst.client.mixin;

import com.amethyst.client.module.CameraState;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
    @Inject(method = "getPerspective", at = @At("HEAD"), cancellable = true)
    private void amethyst$perspective(CallbackInfoReturnable<Perspective> cir) {
        if (CameraState.active()) cir.setReturnValue(Perspective.THIRD_PERSON_BACK);
    }
}
