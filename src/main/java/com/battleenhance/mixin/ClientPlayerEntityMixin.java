package com.battleenhance.mixin;

import com.battleenhance.BattleEnhanceMod;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (message.startsWith("/battleenhance ")) {
            String[] parts = message.split(" ");
            if (parts.length >= 2) {
                String command = parts[1];
                if (command.equals("start")) {
                    BattleEnhanceMod.getLOGGER().info("Manual battle start requested");
                    ci.cancel();
                } else if (command.equals("stop")) {
                    BattleEnhanceMod.INSTANCE.endBattle();
                    ci.cancel();
                }
            }
        }
    }
}
