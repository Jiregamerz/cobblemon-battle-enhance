package com.battleenhance.camera

import com.cobblemon.mod.common.api.events.battles.BattleStartedPostEvent
import com.cobblemon.mod.common.api.events.battles.BattleEndedEvent
import com.battleenhance.BattleEnhanceMod
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK
import net.minecraft.client.Minecraft
import net.minecraft.client.CameraType
import net.minecraft.util.Mth

object CameraController {
    private var isActive = false
    private var targetEntity: net.minecraft.world.entity.Entity? = null

    private var currentX = 0.0
    private var currentY = 0.0
    private var currentZ = 0.0
    private var currentYaw = 0f
    private var transitionProgress = 0f

    private const val CAMERA_DISTANCE = 5.0
    private const val CAMERA_HEIGHT = 2.5
    private const val TRANSITION_SPEED = 0.08f
    private const val FOLLOW_SMOOTHNESS = 0.15f

    private var originalPerspective: CameraType = CameraType.FIRST_PERSON

    fun register() {
        BattleStartedPostEvent.EVENT.register { event ->
            startBattleCamera(event)
        }

        BattleEndedEvent.EVENT.register {
            endBattleCamera()
        }

        END_CLIENT_TICK.register { client ->
            if (isActive) {
                updateCamera(client)
            }
        }
    }

    private fun startBattleCamera(event: BattleStartedPostEvent) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return

        val pokemonEntity = findBattlePokemon(player) ?: return

        targetEntity = pokemonEntity
        isActive = true
        transitionProgress = 0f

        originalPerspective = client.options.cameraType

        client.options.cameraType = CameraType.THIRD_PERSON_BACK

        currentX = player.x
        currentY = player.y + CAMERA_HEIGHT
        currentZ = player.z - CAMERA_DISTANCE
        currentYaw = player.yRot

        BattleEnhanceMod.LOGGER.info("Battle camera started")
    }

    private fun endBattleCamera() {
        val client = Minecraft.getInstance()

        isActive = false
        targetEntity = null
        transitionProgress = 0f

        client.options.cameraType = originalPerspective

        BattleEnhanceMod.LOGGER.info("Battle camera ended")
    }

    private fun updateCamera(client: Minecraft) {
        val player = client.player ?: return
        val target = targetEntity ?: return

        val targetYaw = target.yRot.toDouble()
        val behindX = target.x - Math.sin(Math.toRadians(targetYaw)) * CAMERA_DISTANCE
        val behindZ = target.z + Math.cos(Math.toRadians(targetYaw)) * CAMERA_DISTANCE
        val behindY = target.y + CAMERA_HEIGHT

        if (transitionProgress < 1f) {
            transitionProgress = Mth.clamp(transitionProgress + TRANSITION_SPEED, 0f, 1f)
        }

        currentX = Mth.lerp(FOLLOW_SMOOTHNESS.toDouble(), currentX, behindX)
        currentY = Mth.lerp(FOLLOW_SMOOTHNESS.toDouble(), currentY, behindY)
        currentZ = Mth.lerp(FOLLOW_SMOOTHNESS.toDouble(), currentZ, behindZ)

        val yawDiff = Mth.wrapDegrees(target.yRot - currentYaw)
        currentYaw += yawDiff * FOLLOW_SMOOTHNESS

        client.cameraEntity?.let { cam ->
            cam.setPos(currentX, currentY, currentZ)
            cam.yRot = currentYaw
            cam.xRot = -10f
        }
    }

    private fun findBattlePokemon(player: net.minecraft.world.entity.player.Player): net.minecraft.world.entity.Entity? {
        val world = player.level()
        val nearbyEntities = world.getEntities(
            player,
            player.boundingBox.inflate(32.0)
        ) { entity ->
            entity.javaClass.simpleName.contains("Pokemon") ||
            entity.type.descriptionId.contains("pokemon")
        }

        return nearbyEntities.minByOrNull { it.distanceToSqr(player) }
    }

    fun isActive(): Boolean = isActive
    fun getTargetEntity() = targetEntity
}
