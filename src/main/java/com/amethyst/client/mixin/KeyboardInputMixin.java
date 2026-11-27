package com.amethyst.client.mixin;

import com.amethyst.client.module.ModuleManager;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("RETURN"))
    private void amethyst$tick(CallbackInfo ci) {
        if (!ModuleManager.FREECAM.isEnabled()) return;
        this.playerInput = PlayerInput.DEFAULT;
        this.movementVector = Vec2f.ZERO;
    }
}
