package com.battleenhance.network

import com.battleenhance.BattleEnhanceMod
import com.battleenhance.controller.PokemonController
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Mob

object BattleEnhanceNetworking {
    private val MOVE_ID = ResourceLocation.fromNamespaceAndPath(BattleEnhanceMod.MOD_ID, "move")
    private val ATTACK_ID = ResourceLocation.fromNamespaceAndPath(BattleEnhanceMod.MOD_ID, "attack")
    private val DODGE_ID = ResourceLocation.fromNamespaceAndPath(BattleEnhanceMod.MOD_ID, "dodge")
    private val DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(BattleEnhanceMod.MOD_ID, "damage")

    data class MovePayload(val dx: Double, val dz: Double) : CustomPacketPayload {
        override fun type() = MOVE_TYPE
        companion object {
            val MOVE_TYPE = CustomPacketPayload.Type<MovePayload>(MOVE_ID)
            val CODEC: StreamCodec<FriendlyByteBuf, MovePayload> = StreamCodec.of(
                { buf, p -> buf.writeDouble(p.dx); buf.writeDouble(p.dz) },
                { buf -> MovePayload(buf.readDouble(), buf.readDouble()) }
            )
        }
    }

    data class AttackPayload(val targetId: Int) : CustomPacketPayload {
        override fun type() = ATTACK_TYPE
        companion object {
            val ATTACK_TYPE = CustomPacketPayload.Type<AttackPayload>(ATTACK_ID)
            val CODEC: StreamCodec<FriendlyByteBuf, AttackPayload> = StreamCodec.of(
                { buf, p -> buf.writeInt(p.targetId) },
                { buf -> AttackPayload(buf.readInt()) }
            )
        }
    }

    object DodgePayload : CustomPacketPayload {
        override fun type() = DODGE_TYPE
        val DODGE_TYPE = CustomPacketPayload.Type<DodgePayload>(DODGE_ID)
        val CODEC: StreamCodec<FriendlyByteBuf, DodgePayload> = StreamCodec.of(
            { _, _ -> },
            { DodgePayload }
        )
    }

    data class DamagePayload(val entityId: Int, val amount: Float, val isCritical: Boolean) : CustomPacketPayload {
        override fun type() = DAMAGE_TYPE
        companion object {
            val DAMAGE_TYPE = CustomPacketPayload.Type<DamagePayload>(DAMAGE_ID)
            val CODEC: StreamCodec<FriendlyByteBuf, DamagePayload> = StreamCodec.of(
                { buf, p -> buf.writeInt(p.entityId); buf.writeFloat(p.amount); buf.writeBoolean(p.isCritical) },
                { buf -> DamagePayload(buf.readInt(), buf.readFloat(), buf.readBoolean()) }
            )
        }
    }

    fun register() {
        PayloadTypeRegistry.playC2S().register(MovePayload.MOVE_TYPE, MovePayload.CODEC)
        PayloadTypeRegistry.playC2S().register(AttackPayload.ATTACK_TYPE, AttackPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(DodgePayload.DODGE_TYPE, DodgePayload.CODEC)
        PayloadTypeRegistry.playS2C().register(DamagePayload.DAMAGE_TYPE, DamagePayload.CODEC)

        ServerPlayNetworking.registerGlobalReceiver(MovePayload.MOVE_TYPE) { payload, context ->
            context.server().execute {
                val pokemon = BattleEnhanceMod.controlledPokemon
                if (pokemon != null && !pokemon.level().isClientSide) {
                    PokemonController.handleServerMove(pokemon, payload.dx, payload.dz)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(AttackPayload.ATTACK_TYPE) { payload, context ->
            context.server().execute {
                val pokemon = BattleEnhanceMod.controlledPokemon ?: return@execute
                val server = context.server()

                var target: net.minecraft.world.entity.LivingEntity? = null
                for (player in server.playerList.playerInstances) {
                    val entities = player.level().entities().getAll()
                    for (e in entities) {
                        if (e is net.minecraft.world.entity.LivingEntity && e.id == payload.targetId) {
                            target = e
                            break
                        }
                    }
                    if (target != null) break
                }

                if (target != null) {
                    val damage = pokemon.damageSources().mobAttack(pokemon)
                    target.hurt(damage, 8.0f)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(DodgePayload.DODGE_TYPE) { _, context ->
            context.server().execute {
                val pokemon = BattleEnhanceMod.controlledPokemon ?: return@execute
                if (pokemon is Mob) {
                    pokemon.jumpFromGround()
                    val angle = Math.random() * Math.PI * 2
                    val dodgeX = pokemon.x + Math.cos(angle) * 2.5
                    val dodgeZ = pokemon.z + Math.sin(angle) * 2.5
                    pokemon.navigation.moveTo(dodgeX, pokemon.y, dodgeZ, 0.4)
                }
            }
        }
    }

    fun sendMove(dx: Double, dz: Double) {
        ClientPlayNetworking.send(MovePayload(dx, dz))
    }

    fun sendAttack(targetId: Int) {
        ClientPlayNetworking.send(AttackPayload(targetId))
    }

    fun sendDodge() {
        ClientPlayNetworking.send(DodgePayload)
    }
}
