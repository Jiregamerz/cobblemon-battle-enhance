package com.battleenhance.controller

import com.battleenhance.BattleEnhanceMod
import com.battleenhance.network.BattleEnhanceNetworking
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Mob
import net.minecraft.world.phys.Vec3

object PokemonController {
    private const val MOVE_SPEED = 0.35
    private const val DODGE_SPEED = 0.6

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!BattleEnhanceMod.inBattle) return@register
            val pokemon = BattleEnhanceMod.controlledPokemon ?: return@register
            val player = client.player ?: return@register
            val options = client.options

            val forward = Vec3(-Math.sin(Math.toRadians(player.yRot.toDouble())), 0.0, Math.cos(Math.toRadians(player.yRot.toDouble())))
            val right = Vec3(-Math.sin(Math.toRadians((player.yRot + 90).toDouble())), 0.0, Math.cos(Math.toRadians((player.yRot + 90).toDouble())))

            var moveX = 0.0
            var moveZ = 0.0

            if (options.keyUp.isDown) { moveX += forward.x; moveZ += forward.z }
            if (options.keyDown.isDown) { moveX -= forward.x; moveZ -= forward.z }
            if (options.keyLeft.isDown) { moveX -= right.x; moveZ -= right.z }
            if (options.keyRight.isDown) { moveX += right.x; moveZ += right.z }

            if (moveX != 0.0 || moveZ != 0.0) {
                val dir = Vec3(moveX, 0.0, moveZ).normalize().scale(MOVE_SPEED)
                movePokemon(pokemon, dir)
            }

            while (options.keyJump.consumeClick()) {
                if (pokemon is Mob) {
                    pokemon.jumpFromGround()
                }
            }
        }
    }

    private fun movePokemon(pokemon: net.minecraft.world.entity.LivingEntity, direction: Vec3) {
        val newX = pokemon.x + direction.x
        val newZ = pokemon.z + direction.z
        pokemon.setPos(newX, pokemon.y, newZ)
        if (direction.x != 0.0 || direction.z != 0.0) {
            pokemon.yRot = (Math.toDegrees(Math.atan2(-direction.x, direction.z))).toFloat()
        }
        BattleEnhanceNetworking.sendMove(direction.x, direction.z)
    }

    fun handleServerMove(pokemon: net.minecraft.world.entity.LivingEntity, dx: Double, dz: Double) {
        val dir = Vec3(dx, 0.0, dz).normalize().scale(MOVE_SPEED)
        val newX = pokemon.x + dir.x
        val newZ = pokemon.z + dir.z

        if (pokemon is Mob) {
            pokemon.navigation.moveTo(newX, pokemon.y, newZ, 1.0)
        } else {
            pokemon.setPos(newX, pokemon.y, newZ)
        }

        if (dx != 0.0 || dz != 0.0) {
            pokemon.yRot = (Math.toDegrees(Math.atan2(-dx, dz))).toFloat()
        }
    }
}
