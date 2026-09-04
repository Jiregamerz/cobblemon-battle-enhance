package com.battleenhance.mixin;

import com.battleenhance.camera.CameraController;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Player self = (Player) (Object) this;

        if (self.level().isClientSide()) {
            if (CameraController.INSTANCE.isActive()) {
                // Process movement keys for Pokemon control
            }
        }
    }
}
