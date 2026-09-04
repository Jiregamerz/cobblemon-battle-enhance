package com.battleenhance.ai

import com.cobblemon.mod.common.api.events.battles.BattleStartedPostEvent
import com.cobblemon.mod.common.api.events.battles.BattleEndedEvent
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.*
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages AI behavior for wild and NPC Pokemon during battles
 * Makes Pokemon dodge, attack, and move in real-time
 */
object PokemonAIManager {
    private val activeBattles = ConcurrentHashMap<UUID, BattleAIState>()

    // AI states
    const val STATE_IDLE = 0
    const val STATE_CHASING = 1
    const val STATE_ATTACKING = 2
    const val STATE_DODGING = 3
    const val STATE_FLEEING = 4

    fun register() {
        // Listen for battle events
        BattleStartedPostEvent.EVENT.register { event ->
            startAIForBattle(event)
        }

        BattleEndedEvent.EVENT.register { event ->
            endAIForBattle(event)
        }

        // Server tick to update AI
        ServerTickEvents.END_SERVER_TICK.register { server ->
            server.playerList.playerInstances.forEach { player ->
                activeBattles.forEach { (uuid, state) ->
                    updateAI(state)
                }
            }
        }
    }

    private fun startAIForBattle(event: BattleStartedPostEvent) {
        val battle = event.battle

        // Check if this is a wild battle
        val isWildBattle = battle.isWildBattle

        if (isWildBattle) {
            // Find the wild Pokemon entity
            val wildPokemon = findWildPokemon(battle) ?: return
            val targetPlayer = findTargetPlayer(battle) ?: return

            val state = BattleAIState(
                pokemon = wildPokemon,
                target = targetPlayer,
                state = STATE_CHASING,
                isWild = true
            )

            activeBattles[wildPokemon.uuid] = state
            BattleEnhanceMod.LOGGER.info("AI started for wild Pokemon: ${wildPokemon.name.string}")
        }
    }

    private fun endAIForBattle(event: BattleEndedEvent) {
        // Remove all AI states for this battle
        val iterator = activeBattles.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.value.target.isAlive || entry.value.pokemon.isRemoved) {
                iterator.remove()
            }
        }
    }

    private fun updateAI(state: BattleAIState) {
        val pokemon = state.pokemon
        val target = state.target

        if (!pokemon.isAlive || !target.isAlive || pokemon.isRemoved) {
            return
        }

        // Decrease cooldowns
        if (state.attackCooldown > 0) state.attackCooldown--
        if (state.dodgeCooldown > 0) state.dodgeCooldown--

        val distance = pokemon.squaredDistanceTo(target)

        when (state.state) {
            STATE_CHASING -> updateChasing(state, distance)
            STATE_ATTACKING -> updateAttacking(state, distance)
            STATE_DODGING -> updateDodging(state, distance)
            STATE_FLEEING -> updateFleeing(state, distance)
        }
    }

    private fun updateChasing(state: BattleAIState, distance: Double) {
        val pokemon = state.pokemon as? MobEntity ?: return
        val target = state.target

        // Move toward target
        pokemon.navigation.startMovingTo(target, 1.2)

        // Attack when close enough
        if (distance < 4.0) {
            state.state = STATE_ATTACKING
        }

        // Random dodge
        if (state.dodgeCooldown <= 0 && Math.random() < 0.05) {
            startDodging(state)
        }

        // Flee if low health
        if (state.pokemon.health < state.pokemon.maxHealth * 0.2) {
            state.state = STATE_FLEEING
        }
    }

    private fun updateAttacking(state: BattleAIState, distance: Double) {
        val pokemon = state.pokemon as? MobEntity ?: return
        val target = state.target

        if (state.attackCooldown <= 0) {
            // Perform attack
            performAttack(state)
            state.attackCooldown = 40 + (Math.random() * 40).toInt() // 2-4 seconds

            // Dodge after attack
            if (Math.random() < 0.3) {
                startDodging(state)
            }
        }

        // Back off if too close
        if (distance < 2.0) {
            val away = pokemon.pos.subtract(target.pos).normalize().multiply(3.0)
            pokemon.navigation.startMovingTo(
                pokemon.x + away.x,
                pokemon.y,
                pokemon.z + away.z,
                1.0
            )
        }

        // Return to chase if too far
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
        val pokemon = state.pokemon as? MobEntity ?: return
        val target = state.target

        // Run away
        val away = pokemon.pos.subtract(target.pos).normalize().multiply(10.0)
        pokemon.navigation.startMovingTo(
            pokemon.x + away.x,
            pokemon.y,
            pokemon.z + away.z,
            1.5
        )

        // Stop fleeing if far enough
        if (distance > 25.0) {
            state.state = STATE_CHASING
        }
    }

    private fun startDodging(state: BattleAIState) {
        state.state = STATE_DODGING
        state.dodgeCooldown = 20

        // Jump to dodge
        if (state.pokemon is LivingEntity) {
            state.pokemon.jump()
        }

        // Random side movement
        val angle = Math.random() * Math.PI * 2
        val dodgeX = Math.cos(angle) * 3
        val dodgeZ = Math.sin(angle) * 3

        if (state.pokemon is MobEntity) {
            state.pokemon.navigation.startMovingTo(
                state.pokemon.x + dodgeX,
                state.pokemon.y,
                state.pokemon.z + dodgeZ,
                1.5
            )
        }
    }

    private fun performAttack(state: BattleAIState) {
        val target = state.target

        // Deal damage to player
        if (target is LivingEntity) {
            target.damage(
                state.pokemon.damageSources.mobAttack(state.pokemon),
                5.0f
            )
        }
    }

    private fun findWildPokemon(battle: com.cobblemon.mod.common.api.battles.model.PokemonBattle): PokemonEntity? {
        // Get the wild Pokemon from the battle
        val activePokemon = battle.activePokemon
        for (pokemon in activePokemon) {
            val entity = pokemon.entity
            if (entity is PokemonEntity && entity.ownerUUID == null) {
                return entity
            }
        }
        return null
    }

    private fun findTargetPlayer(battle: com.cobblemon.mod.common.api.battles.model.PokemonBattle): PlayerEntity? {
        // Get the player from the battle
        val actor = battle.actors.firstOrNull()
        return actor?.player
    }

    fun isInBattle(entity: net.minecraft.entity.Entity): Boolean {
        return activeBattles.containsKey(entity.uuid)
    }

    fun getState(entity: net.minecraft.entity.Entity): BattleAIState? {
        return activeBattles[entity.uuid]
    }

    /**
     * AI state for a Pokemon in battle
     */
    data class BattleAIState(
        val pokemon: LivingEntity,
        val target: PlayerEntity,
        var state: Int = STATE_IDLE,
        val isWild: Boolean = true,
        var attackCooldown: Int = 0,
        var dodgeCooldown: Int = 0
    )
}
