package com.battleenhance.integration

import com.battleenhance.BattleEnhanceMod
import com.battleenhance.ai.PokemonAIManager
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.LivingEntity
import java.util.function.Consumer

object CobblemonBridge {
    private var initialized = false
    private var cobblemonLoaded = false

    fun init() {
        if (initialized) return
        initialized = true

        cobblemonLoaded = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cobblemon")
        if (!cobblemonLoaded) {
            BattleEnhanceMod.LOGGER.info("Cobblemon not found")
            return
        }

        BattleEnhanceMod.LOGGER.info("Cobblemon detected! Hooking into battle events...")
        hookEvents()
    }

    private fun hookEvents() {
        try {
            val clazz = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents")

            val startedField = clazz.getDeclaredField("BATTLE_STARTED_POST")
            startedField.isAccessible = true
            val observable = startedField.get(null)

            val subscribeMethod = observable.javaClass.getMethod(
                "subscribe", java.util.function.Consumer::class.java
            )

            val consumer = Consumer<Any> { event ->
                try { handleBattleStarted(event) } catch (e: Exception) {
                    BattleEnhanceMod.LOGGER.warn("Error handling battle start: ${e.message}")
                }
            }
            subscribeMethod.invoke(observable, consumer)
            BattleEnhanceMod.LOGGER.info("Subscribed to BATTLE_STARTED_POST")

            val endedField = clazz.getDeclaredField("BATTLE_VICTORY")
            endedField.isAccessible = true
            val endedObservable = endedField.get(null)
            val endConsumer = Consumer<Any> {
                try { handleBattleEnded() } catch (e: Exception) {
                    BattleEnhanceMod.LOGGER.warn("Error handling battle end: ${e.message}")
                }
            }
            subscribeMethod.invoke(endedObservable, endConsumer)
            BattleEnhanceMod.LOGGER.info("Subscribed to BATTLE_VICTORY")

        } catch (e: Exception) {
            BattleEnhanceMod.LOGGER.warn("Failed to hook Cobblemon events: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun handleBattleStarted(event: Any) {
        val getBattle = event.javaClass.getMethod("getBattle")
        val battle = getBattle.invoke(event) ?: return

        val getActors = battle.javaClass.getMethod("getActors")
        val actors = (getActors.invoke(battle) as? Iterable<*>)?.toList() ?: return
        if (actors.size < 2) return

        val playerEntity = extractEntity(actors[0])
        val enemyEntity = extractEntity(actors[1])

        if (playerEntity != null && enemyEntity != null) {
            Minecraft.getInstance().execute {
                BattleEnhanceMod.startBattle(playerEntity, enemyEntity)
                Minecraft.getInstance().player?.let { player ->
                    PokemonAIManager.startBattle(enemyEntity, player)
                }
            }
            BattleEnhanceMod.LOGGER.info("Battle started! Player: ${playerEntity.name?.string}, Enemy: ${enemyEntity.name?.string}")
        }
    }

    private fun handleBattleEnded() {
        Minecraft.getInstance().execute {
            BattleEnhanceMod.endBattle()
        }
    }

    private fun extractEntity(actor: Any?): LivingEntity? {
        if (actor == null) return null
        try {
            val pokemonList = actor.javaClass.getMethod("getPokemonList").invoke(actor) as? List<*> ?: return null
            if (pokemonList.isEmpty()) return null
            val battlePokemon = pokemonList[0] ?: return null
            val effectedPokemon = battlePokemon.javaClass.getMethod("getEffectedPokemon").invoke(battlePokemon) ?: return null
            return effectedPokemon.javaClass.getMethod("getEntity").invoke(effectedPokemon) as? LivingEntity
        } catch (e: Exception) {
            BattleEnhanceMod.LOGGER.debug("Failed to extract entity: ${e.message}")
            return null
        }
    }
}
