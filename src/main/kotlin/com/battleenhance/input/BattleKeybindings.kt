package com.battleenhance.input

import com.battleenhance.network.BattleEnhanceNetworking
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW

object BattleKeybindings {
    private lateinit var attackKey: KeyMapping
    private lateinit var dodgeKey: KeyMapping
    private lateinit var move1Key: KeyMapping
    private lateinit var move2Key: KeyMapping
    private lateinit var move3Key: KeyMapping
    private lateinit var move4Key: KeyMapping

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

        move1Key = KeyBindingHelper.registerKeyBinding(KeyMapping(
            "key.battleenhance.move1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_1,
            "category.battleenhance"
        ))

        move2Key = KeyBindingHelper.registerKeyBinding(KeyMapping(
            "key.battleenhance.move2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_2,
            "category.battleenhance"
        ))

        move3Key = KeyBindingHelper.registerKeyBinding(KeyMapping(
            "key.battleenhance.move3",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_3,
            "category.battleenhance"
        ))

        move4Key = KeyBindingHelper.registerKeyBinding(KeyMapping(
            "key.battleenhance.move4",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_4,
            "category.battleenhance"
        ))

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (attackKey.consumeClick()) {
                BattleEnhanceNetworking.sendAttack(0)
            }

            while (dodgeKey.consumeClick()) {
                BattleEnhanceNetworking.sendDodge()
            }

            while (move1Key.consumeClick()) {
                BattleEnhanceNetworking.sendAttack(0)
            }

            while (move2Key.consumeClick()) {
                BattleEnhanceNetworking.sendAttack(1)
            }

            while (move3Key.consumeClick()) {
                BattleEnhanceNetworking.sendAttack(2)
            }

            while (move4Key.consumeClick()) {
                BattleEnhanceNetworking.sendAttack(3)
            }
        }
    }
}
