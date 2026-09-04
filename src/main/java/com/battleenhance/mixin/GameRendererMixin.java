package com.battleenhance.mixin;

import com.battleenhance.BattleEnhanceMod;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getMainCamera", at = @At("RETURN"))
    private void onGetCamera(CallbackInfoReturnable<Camera> cir) {
        if (!BattleEnhanceMod.INSTANCE.getInBattle()) return;

        Entity target = BattleEnhanceMod.INSTANCE.getControlledPokemon();
        if (target == null) return;

        Camera camera = cir.getReturnValue();
        camera.setPosition(new net.minecraft.world.phys.Vec3(
            target.getX(),
            target.getY() + 2.0,
            target.getZ()
        ));
    }
}
