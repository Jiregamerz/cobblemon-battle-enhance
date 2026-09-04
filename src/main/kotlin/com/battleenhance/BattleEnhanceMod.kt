package com.battleenhance

import com.battleenhance.camera.CameraController
import com.battleenhance.ai.PokemonAIManager
import com.battleenhance.hud.BattleHUDRenderer
import com.battleenhance.input.BattleKeybindings
import com.battleenhance.network.BattleEnhanceNetworking
import com.battleenhance.integration.CobblemonBridge
import net.fabricmc.api.ModInitializer
import net.minecraft.world.entity.LivingEntity
import org.slf4j.LoggerFactory

class BattleEnhanceMod : ModInitializer {
    companion object {
        const val MOD_ID = "battleenhance"
        @JvmStatic val LOGGER = LoggerFactory.getLogger(MOD_ID)!!

        @JvmStatic var inBattle = false
            private set
        @JvmStatic var controlledPokemon: LivingEntity? = null
            private set
        @JvmStatic var battleTarget: LivingEntity? = null
            private set

        @JvmStatic
        fun startBattle(playerPokemon: LivingEntity, enemy: LivingEntity?) {
            inBattle = true
            controlledPokemon = playerPokemon
            battleTarget = enemy

            CameraController.startBattle(playerPokemon)
            BattleHUDRenderer.start()

            LOGGER.info("Battle started! Controlling: ${playerPokemon.name?.string}")
        }

        @JvmStatic
        fun endBattle() {
            inBattle = false
            controlledPokemon = null
            battleTarget = null

            CameraController.endBattle()
            PokemonAIManager.endAllBattles()
            BattleHUDRenderer.stop()

            LOGGER.info("Battle ended!")
        }
    }

    override fun onInitialize() {
        LOGGER.info("Battle Enhance initializing...")

        BattleEnhanceNetworking.register()
        BattleKeybindings.register()
        CameraController.register()
        PokemonAIManager.register()
        BattleHUDRenderer.register()
        CobblemonBridge.init()

        LOGGER.info("Battle Enhance initialized!")
    }
}
