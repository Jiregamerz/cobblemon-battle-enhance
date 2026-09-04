package com.battleenhance

import com.battleenhance.camera.CameraController
import com.battleenhance.ai.PokemonAIManager
import com.battleenhance.hud.BattleHUDRenderer
import com.battleenhance.input.BattleKeybindings
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

        BattleEnhanceNetworking.register()
        CameraController.register()
        PokemonAIManager.register()
        BattleHUDRenderer.register()
        BattleKeybindings.register()

        BattleEnhanceMod.LOGGER.info("Battle Enhance initialized!")
    }
}
