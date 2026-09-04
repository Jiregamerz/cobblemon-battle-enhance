package com.battleenhance.integration

import com.battleenhance.BattleEnhanceMod
import net.fabricmc.loader.FabricLoader
import net.minecraft.world.entity.LivingEntity
import java.lang.reflect.Method

object CobblemonBridge {
    private var initialized = false
    private var cobblemonLoaded = false

    private var cobblemonEventsClass: Class<*>? = null
    private var battleStartedField: Any? = null
    private var battleEndedField: Any? = null

    fun init() {
        if (initialized) return
        initialized = true

        cobblemonLoaded = FabricLoader.getInstance().isModLoaded("cobblemon")
        if (!cobblemonLoaded) {
            BattleEnhanceMod.LOGGER.info("Cobblemon not found - battle enhance will not auto-trigger")
            return
        }

        BattleEnhanceMod.LOGGER.info("Cobblemon detected! Hooking into battle events via reflection...")
        hookEvents()
    }

    private fun hookEvents() {
        try {
            cobblemonEventsClass = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents")

            val fields = cobblemonEventsClass!!.declaredFields
            for (field in fields) {
                val name = field.name.lowercase()
                when {
                    name.contains("battle_started_post") || name.contains("battlestartedpost") -> {
                        battleStartedField = field.get(null)
                        BattleEnhanceMod.LOGGER.info("Found battle started event field: ${field.name}")
                    }
                    name.contains("battle_ended") || name.contains("battleended") ||
                    name.contains("battle_fainted") || name.contains("battlefainted") -> {
                        if (battleEndedField == null) {
                            battleEndedField = field.get(null)
                            BattleEnhanceMod.LOGGER.info("Found battle ended event field: ${field.name}")
                        }
                    }
                }
            }

            subscribeEvent(battleStartedField, "onBattleStarted")
            subscribeEvent(battleEndedField, "onBattleEnded")

        } catch (e: Exception) {
            BattleEnhanceMod.LOGGER.warn("Failed to hook Cobblemon events: ${e.message}")
            BattleEnhanceMod.LOGGER.info("You will need to manually trigger battles using /battleenhance start")
        }
    }

    private fun subscribeEvent(eventObj: Any?, handlerName: String) {
        if (eventObj == null) return
        try {
            val subscribeMethod = findSubscribeMethod(eventObj.javaClass)
            if (subscribeMethod != null) {
                val handler = createHandler(handlerName)
                if (handler != null) {
                    subscribeMethod.invoke(eventObj, 0, handler)
                    BattleEnhanceMod.LOGGER.info("Successfully subscribed to $handlerName")
                }
            }
        } catch (e: Exception) {
            BattleEnhanceMod.LOGGER.warn("Failed to subscribe to $handlerName: ${e.message}")
        }
    }

    private fun findSubscribeMethod(clazz: Class<*>): Method? {
        var c: Class<*>? = clazz
        while (c != null) {
            for (m in c.declaredMethods) {
                if (m.name == "subscribe" && m.parameterCount == 2) {
                    m.isAccessible = true
                    return m
                }
            }
            c = c.superclass
        }
        return null
    }

    private fun createHandler(name: String): Any? {
        val handlerInterface = try {
            Class.forName("kotlin.jvm.functions.Function1")
        } catch (e: Exception) {
            null
        } ?: return null

        return java.lang.reflect.Proxy.newProxyInstance(
            handlerInterface.classLoader,
            arrayOf(handlerInterface)
        ) { _, method, args ->
            if (name == "onBattleStarted") {
                handleBattleStarted(args?.firstOrNull())
            } else if (name == "onBattleEnded") {
                handleBattleEnded()
            }
            null
        }
    }

    private fun handleBattleStarted(event: Any?) {
        try {
            BattleEnhanceMod.LOGGER.info("Battle detected via Cobblemon event!")
            val battle = getBattleFromEvent(event) ?: return
            val playerPokemon = getPlayerPokemonFromBattle(battle) ?: return
            val enemy = getEnemyPokemonFromBattle(battle)

            net.minecraft.client.Minecraft.getInstance().execute {
                BattleEnhanceMod.startBattle(playerPokemon, enemy)
                if (enemy is LivingEntity) {
                    com.battleenhance.ai.PokemonAIManager.startBattle(enemy, net.minecraft.client.Minecraft.getInstance().player!!)
                }
            }
        } catch (e: Exception) {
            BattleEnhanceMod.LOGGER.warn("Error handling battle start: ${e.message}")
        }
    }

    private fun handleBattleEnded() {
        net.minecraft.client.Minecraft.getInstance().execute {
            BattleEnhanceMod.endBattle()
        }
    }

    private fun getBattleFromEvent(event: Any?): Any? {
        if (event == null) return null
        try {
            val battleField = event.javaClass.getField("battle")
            return battleField.get(event)
        } catch (_: Exception) {}
        try {
            val battleField = event.javaClass.getDeclaredField("battle")
            battleField.isAccessible = true
            return battleField.get(event)
        } catch (_: Exception) {}
        return null
    }

    private fun getPlayerPokemonFromBattle(battle: Any): LivingEntity? {
        try {
            val actorsMethod = battle.javaClass.getMethod("getActors")
            val actors = actorsMethod.invoke(battle) as? Iterable<*> ?: return null

            for (actor in actors) {
                if (actor == null) continue
                val pokemonListMethod = actor.javaClass.getMethod("getActivePokemon")
                val pokemonList = pokemonListMethod.invoke(actor) as? Iterable<*> ?: continue
                for (pokemon in pokemonList) {
                    if (pokemon == null) continue
                    val entityMethod = pokemon.javaClass.getMethod("getEntity")
                    val entity = entityMethod.invoke(pokemon) as? LivingEntity
                    if (entity != null) return entity
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun getEnemyPokemonFromBattle(battle: Any): LivingEntity? {
        try {
            val actorsMethod = battle.javaClass.getMethod("getActors")
            val actors = actorsMethod.invoke(battle) as? Iterable<*> ?: return null
            var foundFirst = false

            for (actor in actors) {
                if (actor == null) continue
                if (!foundFirst) {
                    foundFirst = true
                    continue
                }
                val pokemonListMethod = actor.javaClass.getMethod("getActivePokemon")
                val pokemonList = pokemonListMethod.invoke(actor) as? Iterable<*> ?: continue
                for (pokemon in pokemonList) {
                    if (pokemon == null) continue
                    val entityMethod = pokemon.javaClass.getMethod("getEntity")
                    val entity = entityMethod.invoke(pokemon) as? LivingEntity
                    if (entity != null) return entity
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
