package com.battleenhance.mixin;

import com.battleenhance.camera.CameraController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle Minecraft client state during battles
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRunTick(boolean tick, CallbackInfo ci) {
        // Handle game logic during battle
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(boolean tick, CallbackInfo ci) {
        MinecraftClient self = (MinecraftClient) (Object) this;

        if (CameraController.INSTANCE.isActive()) {
            // Force third-person view during battle
            if (self.options.getPerspective() != net.minecraft.client.option.Perspective.THIRD_PERSON_BACK) {
                self.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
            }
        }
    }
}
