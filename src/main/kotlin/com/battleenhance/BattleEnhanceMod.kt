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

object BattleEnhanceMod : ModInitializer {
    const val MOD_ID = "battleenhance"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)!!

    var inBattle = false
        private set
    var controlledPokemon: LivingEntity? = null
        private set
    var battleTarget: LivingEntity? = null
        private set

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

    fun startBattle(playerPokemon: LivingEntity, enemy: LivingEntity?) {
        inBattle = true
        controlledPokemon = playerPokemon
        battleTarget = enemy

        CameraController.startBattle(playerPokemon)
        BattleHUDRenderer.start()

        LOGGER.info("Battle started! Controlling: ${playerPokemon.name?.string}")
    }

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
