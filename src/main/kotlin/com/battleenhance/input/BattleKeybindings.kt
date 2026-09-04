package com.battleenhance.input

import com.battleenhance.network.BattleEnhanceNetworking
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

/**
 * Handles keybindings for battle controls
 */
object BattleKeybindings {
    private lateinit var attackKey: KeyBinding
    private lateinit var dodgeKey: KeyBinding
    private lateinit var move1Key: KeyBinding
    private lateinit var move2Key: KeyBinding
    private lateinit var move3Key: KeyBinding
    private lateinit var move4Key: KeyBinding

    fun register() {
        // Attack key
        attackKey = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "key.battleenhance.attack",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.battleenhance"
        ))

        // Dodge key
        dodgeKey = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "key.battleenhance.dodge",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE,
            "category.battleenhance"
        ))

        // Move selection keys
        move1Key = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "key.battleenhance.move1",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_1,
            "category.battleenhance"
        ))

        move2Key = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "key.battleenhance.move2",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_2,
            "category.battleenhance"
        ))

        move3Key = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "key.battleenhance.move3",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_3,
            "category.battleenhance"
        ))

        move4Key = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "key.battleenhance.move4",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_4,
            "category.battleenhance"
        ))

        // Register tick handler
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (attackKey.wasPressed()) {
                BattleEnhanceNetworking.sendAttack(0) // Default to first move
            }

            while (dodgeKey.wasPressed()) {
                BattleEnhanceNetworking.sendDodge()
            }

            while (move1Key.wasPressed()) {
                BattleEnhanceNetworking.sendAttack(0)
            }

            while (move2Key.wasPressed()) {
                BattleEnhanceNetworking.sendAttack(1)
            }

            while (move3Key.wasPressed()) {
                BattleEnhanceNetworking.sendAttack(2)
            }

            while (move4Key.wasPressed()) {
                BattleEnhanceNetworking.sendAttack(3)
            }
        }
    }
}
