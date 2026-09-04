package com.battleenhance.mixin;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    // Camera is handled by CameraController.kt
}
