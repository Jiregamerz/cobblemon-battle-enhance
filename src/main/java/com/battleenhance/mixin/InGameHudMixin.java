package com.battleenhance.mixin;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.Gui;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
    // HUD rendering is handled by BattleHUDRenderer via HudRenderCallback
}
