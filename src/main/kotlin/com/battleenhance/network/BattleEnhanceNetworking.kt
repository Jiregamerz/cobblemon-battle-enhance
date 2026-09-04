package com.battleenhance.network

import com.battleenhance.BattleEnhanceMod
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

object BattleEnhanceNetworking {
    private val ATTACK_ID = ResourceLocation.fromNamespaceAndPath(BattleEnhanceMod.MOD_ID, "attack")
    private val DODGE_ID = ResourceLocation.fromNamespaceAndPath(BattleEnhanceMod.MOD_ID, "dodge")

    data class AttackPayload(val moveIndex: Int) : CustomPacketPayload {
        override fun type() = ATTACK_TYPE
        companion object {
            val ATTACK_TYPE = CustomPacketPayload.Type<AttackPayload>(ATTACK_ID)
            val CODEC: StreamCodec<FriendlyByteBuf, AttackPayload> = StreamCodec.of(
                { buf, payload -> buf.writeInt(payload.moveIndex) },
                { buf -> AttackPayload(buf.readInt()) }
            )
        }
    }

    data class DodgePayload(val dummy: Boolean = true) : CustomPacketPayload {
        override fun type() = DODGE_TYPE
        companion object {
            val DODGE_TYPE = CustomPacketPayload.Type<DodgePayload>(DODGE_ID)
            val CODEC: StreamCodec<FriendlyByteBuf, DodgePayload> = StreamCodec.of(
                { _, _ -> },
                { DodgePayload() }
            )
        }
    }

    fun register() {
        PayloadTypeRegistry.playC2S().register(AttackPayload.ATTACK_TYPE, AttackPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(DodgePayload.DODGE_TYPE, DodgePayload.CODEC)

        ServerPlayNetworking.registerGlobalReceiver(AttackPayload.ATTACK_TYPE) { payload, context ->
            val moveIndex = payload.moveIndex
            context.server().execute {
                BattleEnhanceMod.LOGGER.info("Player ${context.player().name.string} used move $moveIndex")
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(DodgePayload.DODGE_TYPE) { _, context ->
            context.server().execute {
                BattleEnhanceMod.LOGGER.info("Player ${context.player().name.string} dodged")
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(AttackPayload.ATTACK_TYPE) { _, context ->
            context.client().execute {
                // Handle attack animation/effect on client
            }
        }
    }

    fun sendAttack(moveIndex: Int) {
        ClientPlayNetworking.send(AttackPayload(moveIndex))
    }

    fun sendDodge() {
        ClientPlayNetworking.send(DodgePayload())
    }
}
