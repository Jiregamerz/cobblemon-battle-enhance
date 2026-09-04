package com.battleenhance

import com.battleenhance.input.BattleKeybindings
import net.fabricmc.api.ClientModInitializer

/**
 * Client-side initialization
 */
class BattleEnhanceClient : ClientModInitializer {
    override fun onInitializeClient() {
        BattleEnhanceMod.LOGGER.info("Battle Enhance client initializing...")

        // Register keybindings (client-only)
        BattleKeybindings.register()

        BattleEnhanceMod.LOGGER.info("Battle Enhance client initialized!")
    }
}
