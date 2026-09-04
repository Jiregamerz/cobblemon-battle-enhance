package com.battleenhance

import com.battleenhance.camera.CameraController
import com.battleenhance.ai.PokemonAIManager
import com.battleenhance.hud.BattleHUDRenderer
import com.battleenhance.input.BattleKeybindings
import com.battleenhance.network.BattleEnhanceNetworking
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object BattleEnhanceMod : ModInitializer {
    const val MOD_ID = "battleenhance"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    var inBattle = false

    override fun onInitialize() {
        LOGGER.info("Battle Enhance initializing...")

        BattleEnhanceNetworking.register()
        BattleKeybindings.register()

        LOGGER.info("Battle Enhance initialized!")
    }
}
