package com.battleenhance.mixin;

import com.battleenhance.BattleEnhanceMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(GuiGraphics context, float tickDelta, CallbackInfo ci) {
        // HUD is rendered via HudRenderCallback in BattleHUDRenderer
    }
}
