package com.battleenhance

import com.battleenhance.camera.CameraController
import com.battleenhance.ai.PokemonAIManager
import com.battleenhance.hud.BattleHUDRenderer
import com.battleenhance.network.BattleEnhanceNetworking
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object BattleEnhanceMod {
    const val MOD_ID = "battleenhance"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)
}

class BattleEnhanceMod : ModInitializer {
    override fun onInitialize() {
        BattleEnhanceMod.LOGGER.info("Battle Enhance initializing...")

        // Register networking
        BattleEnhanceNetworking.register()

        // Register camera controller
        CameraController.register()

        // Register AI manager
        PokemonAIManager.register()

        // Register HUD renderer
        BattleHUDRenderer.register()

        BattleEnhanceMod.LOGGER.info("Battle Enhance initialized!")
    }
}
