package com.battleenhance.ai

import com.battleenhance.BattleEnhanceMod
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2

object PokemonAIManager {
    private val activeBattles = ConcurrentHashMap<UUID, AIState>()

    private const val STATE_IDLE = 0
    private const val STATE_CHASING = 1
    private const val STATE_ATTACKING = 2
    private const val STATE_DODGING = 3
    private const val STATE_FLEEING = 4
    private const val STATE_COOLDOWN = 5

    private const val ATTACK_RANGE = 3.5
    private const val FLEE_HEALTH_PERCENT = 0.25f
    private const val DODGE_CHANCE = 0.04
    private const val ATTACK_COOLDOWN_MIN = 30
    private const val ATTACK_COOLDOWN_MAX = 80
    private const val DODGE_DURATION = 15
    private const val COOLDOWN_DURATION = 20
    private const val CHASE_SPEED = 0.28
    private const val ATTACK_DAMAGE = 8.0f
    private const val FLEE_SPEED = 0.35

    fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            for ((uuid, state) in activeBattles) {
                if (!state.target.isAlive || state.pokemon.isRemoved || state.pokemon.level().isClientSide) {
                    continue
                }
                tickAI(state)
            }

            val iter = activeBattles.entries.iterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                if (!entry.value.target.isAlive || entry.value.pokemon.isRemoved) {
                    iter.remove()
                }
            }
        }
    }

    fun startBattle(enemy: LivingEntity, targetPlayer: Player) {
        val state = AIState(
            pokemon = enemy,
            target = targetPlayer,
            state = STATE_CHASING
        )
        activeBattles[enemy.uuid] = state
        BattleEnhanceMod.LOGGER.info("AI started for: ${enemy.name?.string}")
    }

    fun endAllBattles() {
        activeBattles.clear()
    }

    private fun tickAI(state: AIState) {
        if (state.cooldown > 0) { state.cooldown--; return }
        if (state.dodgeTimer > 0) { state.dodgeTimer--; return }

        val pokemon = state.pokemon
        val target = state.target
        val dist = pokemon.distanceTo(target).toDouble()

        when (state.state) {
            STATE_CHASING -> tickChase(state, dist)
            STATE_ATTACKING -> tickAttack(state, dist)
            STATE_DODGING -> tickDodge(state)
            STATE_FLEEING -> tickFlee(state, dist)
        }
    }

    private fun tickChase(state: AIState, dist: Double) {
        val mob = state.pokemon as? Mob ?: return

        mob.navigation.moveTo(state.target, CHASE_SPEED)
        faceEntity(state.pokemon, state.target)

        if (dist <= ATTACK_RANGE) {
            state.state = STATE_ATTACKING
        }

        if (Math.random() < DODGE_CHANCE && state.dodgeTimer <= 0) {
            startDodge(state)
        }

        if (state.pokemon.health <= state.pokemon.maxHealth.toDouble() * FLEE_HEALTH_PERCENT) {
            state.state = STATE_FLEEING
        }
    }

    private fun tickAttack(state: AIState, dist: Double) {
        faceEntity(state.pokemon, state.target)

        if (dist > ATTACK_RANGE * 1.5) {
            state.state = STATE_CHASING
            return
        }

        performAttack(state)
        state.cooldown = ATTACK_COOLDOWN_MIN + (Math.random() * (ATTACK_COOLDOWN_MAX - ATTACK_COOLDOWN_MIN)).toInt()

        if (Math.random() < 0.35) {
            startDodge(state)
        }
    }

    private fun tickDodge(state: AIState) {
        if (state.dodgeTimer <= 0) {
            state.state = STATE_CHASING
        }
    }

    private fun tickFlee(state: AIState, dist: Double) {
        val mob = state.pokemon as? Mob ?: return

        val awayX = state.pokemon.x - state.target.x
        val awayZ = state.pokemon.z - state.target.z
        val len = Math.sqrt(awayX * awayX + awayZ * awayZ)
        if (len > 0) {
            val fleeX = state.pokemon.x + (awayX / len) * FLEE_SPEED
            val fleeZ = state.pokemon.z + (awayZ / len) * FLEE_SPEED
            mob.navigation.moveTo(fleeX, state.pokemon.y, fleeZ, FLEE_SPEED)
        }

        if (dist > 20.0 || state.pokemon.health > state.pokemon.maxHealth * 0.5f) {
            state.state = STATE_CHASING
        }
    }

    private fun performAttack(state: AIState) {
        val target = state.target
        if (target is LivingEntity && target.isAlive) {
            val source = state.pokemon.damageSources().mobAttack(state.pokemon)
            target.hurt(source, ATTACK_DAMAGE)

            if (state.pokemon.level() is ServerLevel) {
                val level = state.pokemon.level() as ServerLevel
                level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.CRIT,
                    target.x, target.y + 1.0, target.z,
                    10, 0.3, 0.3, 0.3, 0.05
                )
            }
        }
    }

    private fun startDodge(state: AIState) {
        state.state = STATE_DODGING
        state.dodgeTimer = DODGE_DURATION

        if (state.pokemon is LivingEntity) {
            state.pokemon.jumpFromGround()
        }

        val mob = state.pokemon as? Mob ?: return
        val angle = Math.random() * Math.PI * 2
        val dodgeX = state.pokemon.x + Math.cos(angle) * 3
        val dodgeZ = state.pokemon.z + Math.sin(angle) * 3
        mob.navigation.moveTo(dodgeX, state.pokemon.y, dodgeZ, 0.4)
    }

    private fun faceEntity(entity: LivingEntity, target: LivingEntity) {
        val dx = target.x - entity.x
        val dz = target.z - entity.z
        entity.yRot = Math.toDegrees(atan2(-dx, dz)).toFloat()
    }

    fun isInBattle(entity: net.minecraft.world.entity.Entity) = activeBattles.containsKey(entity.uuid)

    data class AIState(
        val pokemon: LivingEntity,
        val target: Player,
        var state: Int = STATE_IDLE,
        var cooldown: Int = 0,
        var dodgeTimer: Int = 0
    )
}
