package com.battleenhance.camera

import com.battleenhance.BattleEnhanceMod
import com.battleenhance.integration.CobblemonBridge
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
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
    private var currentYaw = 0.0
    private var currentPitch = 0.0
    private var transitionProgress = 0.0

    private const val CAMERA_DISTANCE = 4.0
    private const val CAMERA_HEIGHT = 2.0
    private const val CAMERA_PITCH = -8.0
    private const val TRANSITION_SPEED = 0.06
    private const val FOLLOW_SMOOTHNESS = 0.2
    private const val YAW_SMOOTHNESS = 0.15

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

    fun startBattle(pokemon: Entity) {
        if (active) return

        targetEntity = pokemon
        active = true
        transitionProgress = 0.0

        val client = Minecraft.getInstance()
        originalPerspective = client.options.cameraType

        client.options.cameraType = CameraType.THIRD_PERSON_BACK

        val player = client.player ?: return
        currentX = player.x
        currentY = player.y + 1.0
        currentZ = player.z
        currentYaw = player.yRot.toDouble()
        currentPitch = CAMERA_PITCH
    }

    fun endBattle() {
        if (!active) return

        targetEntity = null
        active = false
        transitionProgress = 0.0

        val client = Minecraft.getInstance()
        client.options.cameraType = CameraType.FIRST_PERSON
        client.cameraEntity = client.player
    }

    private fun tick(client: Minecraft) {
        val target = targetEntity ?: return
        if (target.isRemoved) {
            endBattle()
            return
        }

        transitionProgress = Math.min(1.0, transitionProgress + TRANSITION_SPEED)

        val player = client.player ?: return

        val direction = target.position().subtract(player.position()).normalize()
        val behindX = target.x - direction.x * CAMERA_DISTANCE
        val behindY = target.y + CAMERA_HEIGHT
        val behindZ = target.z - direction.z * CAMERA_DISTANCE

        val smooth = FOLLOW_SMOOTHNESS * transitionProgress
        currentX = Mth.lerp(smooth, currentX, behindX)
        currentY = Mth.lerp(smooth, currentY, behindY)
        currentZ = Mth.lerp(smooth, currentZ, behindZ)

        val yawDiff = Mth.wrapDegrees((target.yRot - currentYaw).toFloat()).toDouble()
        currentYaw += yawDiff * YAW_SMOOTHNESS * transitionProgress
        currentPitch = Mth.lerp(smooth, currentPitch, CAMERA_PITCH)

        client.cameraEntity?.setPos(currentX, currentY, currentZ)
        client.cameraEntity?.let { cam ->
            cam.yRot = currentYaw.toFloat()
            cam.xRot = currentPitch.toFloat()
        }
        try {
            val camera = client.gameRenderer.mainCamera
            val bobField = camera.javaClass.getDeclaredField("bob")
            bobField.isAccessible = true
            bobField.setDouble(camera, 0.0)
            val oBobField = camera.javaClass.getDeclaredField("oBob")
            oBobField.isAccessible = true
            oBobField.setDouble(camera, 0.0)
        } catch (_: Exception) { }
    }

    fun findPlayerPokemon(player: Player): Entity? {
        if (!CobblemonBridge.isLoaded()) return null
        return CobblemonBridge.findPlayerPokemon(player)
    }
}
