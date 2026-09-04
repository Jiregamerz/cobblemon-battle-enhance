package com.battleenhance.input

import com.battleenhance.BattleEnhanceMod
import com.battleenhance.network.BattleEnhanceNetworking
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB

object BattleKeybindings {
    private lateinit var attackKey: KeyMapping
    private lateinit var dodgeKey: KeyMapping

    fun register() {
        attackKey = KeyBindingHelper.registerKeyBinding(KeyMapping(
            "key.battleenhance.attack",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.battleenhance"
        ))

        dodgeKey = KeyBindingHelper.registerKeyBinding(KeyMapping(
            "key.battleenhance.dodge",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE,
            "category.battleenhance"
        ))

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!BattleEnhanceMod.inBattle) return@register
            val player = client.player ?: return@register
            val pokemon = BattleEnhanceMod.controlledPokemon ?: return@register

            while (attackKey.consumeClick()) {
                val target = findNearestEnemy(player, pokemon)
                if (target != null) {
                    BattleEnhanceNetworking.sendAttack(target.id)
                }
            }

            while (dodgeKey.consumeClick()) {
                BattleEnhanceNetworking.sendDodge()
            }
        }
    }

    private fun findNearestEnemy(player: net.minecraft.world.entity.player.Player, pokemon: Entity): Entity? {
        val world = player.level()
        val box = AABB(
            player.x - 16, player.y - 8, player.z - 16,
            player.x + 16, player.y + 8, player.z + 16
        )
        val entities = world.getEntities(player, box) { entity ->
            entity is net.minecraft.world.entity.LivingEntity &&
            entity != pokemon &&
            entity != player &&
            !entity.isRemoved &&
            entity.javaClass.simpleName.contains("Pokemon")
        }
        return entities.minByOrNull { it.distanceToSqr(pokemon) }
    }
}
