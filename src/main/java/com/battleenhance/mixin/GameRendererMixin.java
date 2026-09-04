package com.battleenhance.mixin;

import com.battleenhance.camera.CameraController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRender(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        if (CameraController.INSTANCE.isActive() && client.player != null) {
            // Camera updates are handled in CameraController
        }
    }
}
