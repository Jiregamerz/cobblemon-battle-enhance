package com.battleenhance.mixin;

import com.battleenhance.BattleEnhanceMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide()) return;
        if (!BattleEnhanceMod.INSTANCE.getInBattle()) return;

        Object pokemon = BattleEnhanceMod.INSTANCE.getControlledPokemon();
        if (pokemon instanceof Mob mob) {
            double dx = 0;
            double dz = 0;

            var options = net.minecraft.client.Minecraft.getInstance().options;
            double yRot = self.getYRot();
            Vec3 forward = new Vec3(-Math.sin(Math.toRadians(yRot)), 0, Math.cos(Math.toRadians(yRot)));
            Vec3 right = new Vec3(-Math.sin(Math.toRadians(yRot + 90)), 0, Math.cos(Math.toRadians(yRot + 90)));

            if (options.keyUp.isDown()) { dx += forward.x; dz += forward.z; }
            if (options.keyDown.isDown()) { dx -= forward.x; dz -= forward.z; }
            if (options.keyLeft.isDown()) { dx -= right.x; dz -= right.z; }
            if (options.keyRight.isDown()) { dx += right.x; dz += right.z; }

            if (dx != 0 || dz != 0) {
                var len = Math.sqrt(dx * dx + dz * dz);
                dx = dx / len * 0.35;
                dz = dz / len * 0.35;
                mob.setDeltaMovement(dx, mob.getDeltaMovement().y, dz);
                mob.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
                mob.hurtMarked = true;
            }
        }
    }
}
