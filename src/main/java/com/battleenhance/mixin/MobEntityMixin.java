package com.battleenhance.mixin;

import com.battleenhance.BattleEnhanceMod;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobEntityMixin {

    @Inject(method = "registerGoals", at = @At("RETURN"))
    private void onInitGoals(CallbackInfo ci) {
        // AI goals are managed by PokemonAIManager
    }
}
