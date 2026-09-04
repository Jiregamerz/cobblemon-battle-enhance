package com.battleenhance.mixin;

import com.battleenhance.BattleEnhanceMod;
import com.battleenhance.hud.BattleHUDRenderer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "hurt", at = @At("RETURN"))
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self.level().isClientSide() && cir.getReturnValue()) {
            Object controlled = BattleEnhanceMod.getControlledPokemon();
            if (self == controlled) {
                BattleHUDRenderer.INSTANCE.addDamage(amount);
            }

            Object enemy = BattleEnhanceMod.getBattleTarget();
            if (self == enemy) {
                BattleHUDRenderer.INSTANCE.addEnemyDamage(amount);
            }
        }
    }
}
