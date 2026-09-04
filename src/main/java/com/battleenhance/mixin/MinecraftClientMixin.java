package com.battleenhance.mixin;

import com.battleenhance.BattleEnhanceMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen == null) return;
        if (!BattleEnhanceMod.getInBattle()) return;

        String className = screen.getClass().getName();
        if (className.contains("BattleOverlay") || className.contains("BattleMenu") || className.contains("BattleChoice")) {
            BattleEnhanceMod.LOGGER.info("Blocked Cobblemon battle screen: {}", className);
            ci.cancel();
        }
    }
}
