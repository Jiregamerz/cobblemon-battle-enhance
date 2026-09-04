package com.battleenhance.camera

import com.battleenhance.BattleEnhanceMod
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.CameraType
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

object CameraController {
    private var active = false
    private var targetEntity: Entity? = null
    private var originalPerspective: CameraType = CameraType.FIRST_PERSON

    private var currentX = 0.0
    private var currentY = 0.0
    private var currentZ = 0.0
    private var currentYaw = 0f
    private var currentPitch = 0f
    private var transitionProgress = 0f

    private const val CAMERA_DISTANCE = 4.0
    private const val CAMERA_HEIGHT = 2.0
    private const val CAMERA_PITCH = -8f
    private const val TRANSITION_SPEED = 0.06f
    private const val FOLLOW_SMOOTHNESS = 0.2f
    private const val YAW_SMOOTHNESS = 0.15f

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (BattleEnhanceMod.inBattle && !active) {
                val target = BattleEnhanceMod.controlledPokemon
                if (target != null) {
                    startBattle(target)
                }
            } else if (!BattleEnhanceMod.inBattle && active) {
                endBattle()
            }

            if (active) {
                tick(client)
            }
        }
    }

    fun startBattle(target: Entity) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return

        targetEntity = target
        active = true
        transitionProgress = 0f

        originalPerspective = client.options.cameraType
        if (client.options.cameraType != CameraType.THIRD_PERSON_BACK) {
            client.options.cameraType = CameraType.THIRD_PERSON_BACK
        }

        currentX = player.x
        currentY = player.y + CAMERA_HEIGHT
        currentZ = player.z - CAMERA_DISTANCE
        currentYaw = target.yRot
        currentPitch = CAMERA_PITCH

        BattleEnhanceMod.LOGGER.info("Camera: following ${target.name?.string}")
    }

    fun endBattle() {
        val client = Minecraft.getInstance()

        active = false
        targetEntity = null
        transitionProgress = 0f

        client.options.cameraType = originalPerspective
    }

    private fun tick(client: Minecraft) {
        val target = targetEntity ?: return
        val player = client.player ?: return

        if (!target.isAlive || target.isRemoved) {
            BattleEnhanceMod.endBattle()
            return
        }

        val behindX = target.x - Math.sin(Math.toRadians(target.yRot.toDouble())) * CAMERA_DISTANCE
        val behindZ = target.z + Math.cos(Math.toRadians(target.yRot.toDouble())) * CAMERA_DISTANCE
        val behindY = target.y + CAMERA_HEIGHT

        if (transitionProgress < 1f) {
            transitionProgress = Mth.clamp(transitionProgress + TRANSITION_SPEED, 0f, 1f)
        }

        val smooth = FOLLOW_SMOOTHNESS * transitionProgress.toDouble()
        currentX = Mth.lerp(smooth, currentX, behindX)
        currentY = Mth.lerp(smooth, currentY, behindY)
        currentZ = Mth.lerp(smooth, currentZ, behindZ)

        val yawDiff = Mth.wrapDegrees(target.yRot - currentYaw)
        currentYaw += (yawDiff * YAW_SMOOTHNESS * transitionProgress).toFloat()
        currentPitch = Mth.lerp(smooth, currentPitch, CAMERA_PITCH)

        client.cameraEntity?.let { cam ->
            cam.setPos(currentX, currentY, currentZ)
            cam.yRot = currentYaw
            cam.xRot = currentPitch
            cam.yBob = 0f
            cam.xBob = 0f
        }
    }

    fun findPlayerPokemon(player: Player): Entity? {
        val world = player.level()
        val nearby = world.getEntities(player, player.boundingBox.inflate(16.0)) { entity ->
            entity.javaClass.simpleName.contains("Pokemon") ||
            entity.javaClass.name.contains("pokemon") ||
            entity.type.descriptionId.contains("pokemon")
        }
        return nearby.minByOrNull { it.distanceToSqr(player) }
    }

    fun isActive() = active
    fun getTarget() = targetEntity
}
