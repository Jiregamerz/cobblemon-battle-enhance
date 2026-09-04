package com.battleenhance.ai

import com.battleenhance.BattleEnhanceMod
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PokemonAIManager {
    private val activeBattles = ConcurrentHashMap<UUID, BattleAIState>()

    const val STATE_IDLE = 0
    const val STATE_CHASING = 1
    const val STATE_ATTACKING = 2
    const val STATE_DODGING = 3
    const val STATE_FLEEING = 4

    fun register() {
        ServerTickEvents.END_SERVER_TICK.register {
            activeBattles.forEach { (_, state) ->
                updateAI(state)
            }

            val iterator = activeBattles.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (!entry.value.target.isAlive || entry.value.pokemon.isRemoved) {
                    iterator.remove()
                }
            }
        }
    }

    fun startBattle(pokemon: LivingEntity, target: Player) {
        val state = BattleAIState(
            pokemon = pokemon,
            target = target,
            state = STATE_CHASING,
            isWild = true
        )
        activeBattles[pokemon.uuid] = state
        BattleEnhanceMod.LOGGER.info("AI started for Pokemon: ${pokemon.name?.string}")
    }

    fun endAllBattles() {
        activeBattles.clear()
    }

    private fun updateAI(state: BattleAIState) {
        val pokemon = state.pokemon
        val target = state.target

        if (!pokemon.isAlive || !target.isAlive || pokemon.isRemoved) {
            return
        }

        if (state.attackCooldown > 0) state.attackCooldown--
        if (state.dodgeCooldown > 0) state.dodgeCooldown--

        val distance = pokemon.distanceToSqr(target)

        when (state.state) {
            STATE_CHASING -> updateChasing(state, distance)
            STATE_ATTACKING -> updateAttacking(state, distance)
            STATE_DODGING -> updateDodging(state, distance)
            STATE_FLEEING -> updateFleeing(state, distance)
        }
    }

    private fun updateChasing(state: BattleAIState, distance: Double) {
        val pokemon = state.pokemon as? Mob ?: return

        pokemon.navigation.moveTo(state.target, 1.2)

        if (distance < 4.0) {
            state.state = STATE_ATTACKING
        }

        if (state.dodgeCooldown <= 0 && Math.random() < 0.05) {
            startDodging(state)
        }

        if (state.pokemon.health < state.pokemon.maxHealth * 0.2f) {
            state.state = STATE_FLEEING
        }
    }

    private fun updateAttacking(state: BattleAIState, distance: Double) {
        val pokemon = state.pokemon as? Mob ?: return

        if (state.attackCooldown <= 0) {
            performAttack(state)
            state.attackCooldown = 40 + (Math.random() * 40).toInt()

            if (Math.random() < 0.3) {
                startDodging(state)
            }
        }

        if (distance < 2.0) {
            val away = pokemon.position().subtract(state.target.position()).normalize().scale(3.0)
            pokemon.navigation.moveTo(
                pokemon.x + away.x,
                pokemon.y,
                pokemon.z + away.z,
                1.0
            )
        }

        if (distance > 10.0) {
            state.state = STATE_CHASING
        }
    }

    private fun updateDodging(state: BattleAIState, distance: Double) {
        if (state.dodgeCooldown <= 0) {
            state.state = STATE_CHASING
        }
    }

    private fun updateFleeing(state: BattleAIState, distance: Double) {
        val pokemon = state.pokemon as? Mob ?: return

        val away = pokemon.position().subtract(state.target.position()).normalize().scale(10.0)
        pokemon.navigation.moveTo(
            pokemon.x + away.x,
            pokemon.y,
            pokemon.z + away.z,
            1.5
        )

        if (distance > 25.0) {
            state.state = STATE_CHASING
        }
    }

    private fun startDodging(state: BattleAIState) {
        state.state = STATE_DODGING
        state.dodgeCooldown = 20

        if (state.pokemon is LivingEntity) {
            state.pokemon.jumpFromGround()
        }

        val angle = Math.random() * Math.PI * 2
        val dodgeX = Math.cos(angle) * 3
        val dodgeZ = Math.sin(angle) * 3

        if (state.pokemon is Mob) {
            state.pokemon.navigation.moveTo(
                state.pokemon.x + dodgeX,
                state.pokemon.y,
                state.pokemon.z + dodgeZ,
                1.5
            )
        }
    }

    private fun performAttack(state: BattleAIState) {
        val target = state.target

        if (target is LivingEntity) {
            target.hurt(
                state.pokemon.damageSources().mobAttack(state.pokemon),
                5.0f
            )
        }
    }

    fun isInBattle(entity: net.minecraft.world.entity.Entity): Boolean {
        return activeBattles.containsKey(entity.uuid)
    }

    fun getState(entity: net.minecraft.world.entity.Entity): BattleAIState? {
        return activeBattles[entity.uuid]
    }

    data class BattleAIState(
        val pokemon: LivingEntity,
        val target: Player,
        var state: Int = STATE_IDLE,
        val isWild: Boolean = true,
        var attackCooldown: Int = 0,
        var dodgeCooldown: Int = 0
    )
}
