package com.battleenhance.camera

import com.cobblemon.mod.common.api.events.battles.BattleStartedPostEvent
import com.cobblemon.mod.common.api.events.battles.BattleEndedEvent
import net.fabricmc.fabric.api.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.Perspective
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

/**
 * Controls camera during Pokemon battles
 * Smoothly transitions to third-person view behind Pokemon
 */
object CameraController {
    private var isActive = false
    private var targetEntity: net.minecraft.entity.Entity? = null

    // Camera interpolation
    private var currentX = 0.0
    private var currentY = 0.0
    private var currentZ = 0.0
    private var currentYaw = 0f
    private var currentPitch = 0f
    private var transitionProgress = 0f

    // Camera settings
    private const val CAMERA_DISTANCE = 5.0
    private const val CAMERA_HEIGHT = 2.5
    private const val TRANSITION_SPEED = 0.08f
    private const val FOLLOW_SMOOTHNESS = 0.15f

    // Original perspective to restore
    private var originalPerspective: Perspective = Perspective.FIRST_PERSON

    fun register() {
        // Listen for battle start
        BattleStartedPostEvent.EVENT.register { event ->
            startBattleCamera(event)
        }

        // Listen for battle end
        BattleEndedEvent.EVENT.register { event ->
            endBattleCamera()
        }

        // Client tick to update camera
        END_CLIENT_TICK.register { client ->
            if (isActive) {
                updateCamera(client)
            }
        }
    }

    private fun startBattleCamera(event: BattleStartedPostEvent) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        // Find the Pokemon entity to follow
        // In wild battles, find the wild Pokemon
        // In trainer battles, find the player's active Pokemon
        val pokemonEntity = findBattlePokemon(player) ?: return

        targetEntity = pokemonEntity
        isActive = true
        transitionProgress = 0f

        // Save original perspective
        originalPerspective = client.options.perspective

        // Switch to third-person back
        client.options.perspective = Perspective.THIRD_PERSON_BACK

        // Initialize camera position
        currentX = player.x
        currentY = player.y + CAMERA_HEIGHT
        currentZ = player.z - CAMERA_DISTANCE
        currentYaw = player.yaw
        currentPitch = 0f

        BattleEnhanceMod.LOGGER.info("Battle camera started")
    }

    private fun endBattleCamera() {
        val client = MinecraftClient.getInstance()

        isActive = false
        targetEntity = null
        transitionProgress = 0f

        // Restore original perspective
        client.options.perspective = originalPerspective

        BattleEnhanceMod.LOGGER.info("Battle camera ended")
    }

    private fun updateCamera(client: MinecraftClient) {
        val player = client.player ?: return
        val target = targetEntity ?: return

        // Calculate target position behind the Pokemon
        val targetYaw = target.yaw.toDouble()
        val behindX = target.x - Math.sin(Math.toRadians(targetYaw.toDouble())) * CAMERA_DISTANCE
        val behindZ = target.z + Math.cos(Math.toRadians(targetYaw.toDouble())) * CAMERA_DISTANCE
        val behindY = target.y + CAMERA_HEIGHT

        // Smooth interpolation
        if (transitionProgress < 1f) {
            transitionProgress = MathHelper.clamp(transitionProgress + TRANSITION_SPEED, 0f, 1f)
        }

        // Lerp camera position
        currentX = MathHelper.lerp(FOLLOW_SMOOTHNESS.toDouble(), currentX, behindX)
        currentY = MathHelper.lerp(FOLLOW_SMOOTHNESS.toDouble(), currentY, behindY)
        currentZ = MathHelper.lerp(FOLLOW_SMOOTHNESS.toDouble(), currentZ, behindZ)

        // Smooth yaw
        val yawDiff = MathHelper.wrapDegrees(target.yaw - currentYaw)
        currentYaw += yawDiff * FOLLOW_SMOOTHNESS

        // Apply camera position
        // We set the camera entity position, not the player
        client.cameraEntity?.let { cam ->
            cam.setPos(currentX, currentY, currentZ)
            cam.yaw = currentYaw
            cam.pitch = -10f // Slight downward angle
        }
    }

    private fun findBattlePokemon(player: net.minecraft.entity.player.PlayerEntity): net.minecraft.entity.Entity? {
        // Search for nearby PokemonEntity that is battling
        val world = player.world
        val nearbyEntities = world.getEntitiesByClass(
            net.minecraft.entity.Entity::class.java,
            player.boundingBox.expand(32.0)
        ) { entity ->
            entity.javaClass.simpleName.contains("Pokemon") ||
            entity.type.registryEntry?.registry?.key?.value?.path?.contains("pokemon") == true
        }

        // Return the closest one
        return nearbyEntities.minByOrNull { it.squaredDistanceTo(player) }
    }

    fun isActive(): Boolean = isActive
    fun getTargetEntity() = targetEntity
}
