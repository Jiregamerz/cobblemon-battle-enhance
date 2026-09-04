package com.battleenhance.network

import com.battleenhance.BattleEnhanceMod
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.util.Identifier

/**
 * Handles network communication for battle enhancements
 */
object BattleEnhanceNetworking {
    val ATTACK_PACKET = Identifier.of(BattleEnhanceMod.MOD_ID, "attack")
    val DODGE_PACKET = Identifier.of(BattleEnhanceMod.MOD_ID, "dodge")
    val MOVE_SELECT_PACKET = Identifier.of(BattleEnhanceMod.MOD_ID, "move_select")

    fun register() {
        // Register server-side packet handlers
        ServerPlayNetworking.registerGlobalReceiver(ATTACK_PACKET) { server, player, handler, buf, responseSender ->
            val moveIndex = buf.readInt()
            server.execute {
                // Handle attack
                BattleEnhanceMod.LOGGER.info("Player ${player.name.string} used move $moveIndex")
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(DODGE_PACKET) { server, player, handler, buf, responseSender ->
            server.execute {
                // Handle dodge
                BattleEnhanceMod.LOGGER.info("Player ${player.name.string} dodged")
            }
        }

        // Register client-side packet handlers (for receiving from server)
        ClientPlayNetworking.registerGlobalReceiver(ATTACK_PACKET) { client, handler, buf, responseSender ->
            val entityId = buf.readInt()
            val damage = buf.readFloat()
            client.execute {
                // Handle attack animation/effect on client
            }
        }
    }

    /**
     * Send attack packet to server
     */
    fun sendAttack(moveIndex: Int) {
        ClientPlayNetworking.send(ATTACK_PACKET) { buf ->
            buf.writeInt(moveIndex)
        }
    }

    /**
     * Send dodge packet to server
     */
    fun sendDodge() {
        ClientPlayNetworking.send(DODGE_PACKET) {}
    }
}
