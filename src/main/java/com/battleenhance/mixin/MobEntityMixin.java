package com.battleenhance.mixin;

import com.battleenhance.ai.PokemonAIManager;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobEntityMixin {

    @Inject(method = "registerGoals", at = @At("RETURN"))
    private void onInitGoals(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;

        if (PokemonAIManager.INSTANCE.isInBattle(self)) {
            // Clear existing goals and add battle-specific ones
        }
    }
}
