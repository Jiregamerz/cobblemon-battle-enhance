package com.battleenhance.mixin;

import com.battleenhance.camera.CameraController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onRunTick(CallbackInfo ci) {
        // Handle game logic during battle
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onRender(CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;

        if (CameraController.INSTANCE.isActive()) {
            if (self.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
                self.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        }
    }
}
