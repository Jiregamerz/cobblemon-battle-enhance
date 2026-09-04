package com.battleenhance.hud

import com.battleenhance.BattleEnhanceMod
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.entity.LivingEntity
import java.util.concurrent.ConcurrentLinkedQueue

object BattleHUDRenderer {
    private var active = false
    private val damageNumbers = ConcurrentLinkedQueue<DamageData>()

    private const val HP_BAR_WIDTH = 160
    private const val HP_BAR_HEIGHT = 10
    private const val HP_BG = 0x202020
    private const val HP_BORDER = 0x555555

    fun register() {
        HudRenderCallback.EVENT.register { context, _ ->
            if (active) render(context)
        }
    }

    fun start() {
        active = true
        damageNumbers.clear()
    }

    fun stop() {
        active = false
        damageNumbers.clear()
    }

    private fun render(context: GuiGraphics) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val font = client.font
        val sw = client.window.guiScaledWidth

        val pokemon = BattleEnhanceMod.controlledPokemon
        if (pokemon != null) {
            renderPokemonHP(context, pokemon, sw, 30, font, "Your Pokemon")
        }

        val enemy = BattleEnhanceMod.battleTarget
        if (enemy != null && enemy.isAlive) {
            renderPokemonHP(context, enemy, sw, 60, font, "Enemy")
        }

        renderEnemyAIStatus(context, sw, font)

        val iter = damageNumbers.iterator()
        while (iter.hasNext()) {
            val d = iter.next()
            val age = System.currentTimeMillis() - d.createdAt
            if (age > 2000) { iter.remove(); continue }
            renderDamage(context, d, age, sw, font)
        }
    }

    private fun renderPokemonHP(context: GuiGraphics, entity: LivingEntity, sw: Int, baseY: Int,
                                 font: net.minecraft.client.gui.Font, label: String) {
        val x = (sw - HP_BAR_WIDTH) / 2
        val y = baseY

        val hp = entity.health
        val maxHp = entity.maxHealth
        val ratio = (hp / maxHp).coerceIn(0f, 1f)

        val color = when {
            ratio > 0.5f -> 0x44CC44
            ratio > 0.25f -> 0xCCCC44
            else -> 0xCC4444
        }

        context.fill(x - 1, y - 1, x + HP_BAR_WIDTH + 1, y + HP_BAR_HEIGHT + 1, HP_BORDER)
        context.fill(x, y, x + HP_BAR_WIDTH, y + HP_BAR_HEIGHT, HP_BG)
        context.fill(x, y, x + (HP_BAR_WIDTH * ratio).toInt(), y + HP_BAR_HEIGHT, color)

        val name = entity.name?.string ?: "Unknown"
        val hpText = "%.0f / %.0f".format(hp, maxHp)

        context.drawString(font, Component.literal("$label: $name").withStyle(ChatFormatting.BOLD),
            x, y - 12, 0xFFFFFF, true)
        context.drawString(font, Component.literal(hpText),
            x + HP_BAR_WIDTH + 8, y + 1, 0xCCCCCC, true)
    }

    private fun renderEnemyAIStatus(context: GuiGraphics, sw: Int, font: net.minecraft.client.gui.Font) {
        val enemy = BattleEnhanceMod.battleTarget ?: return
        if (!enemy.isAlive) return

        val x = sw / 2 - 40
        val y = 80
        context.drawString(font, Component.literal("Enemy: Active").withStyle(ChatFormatting.GRAY),
            x, y, 0xAAAAAA, true)
    }

    private fun renderDamage(context: GuiGraphics, d: DamageData, age: Long, sw: Int,
                             font: net.minecraft.client.gui.Font) {
        val x = sw / 2 + ((Math.random() - 0.5) * 20).toInt()
        val y = 140 - (age * 0.04).toInt()

        val text = "-%.0f".format(d.amount)
        val alpha = (1.0 - age / 2000.0).coerceIn(0.0, 1.0)
        val color = ((alpha * 255).toInt() shl 24) or 0xFF4444

        context.drawString(font, Component.literal(text).withStyle(ChatFormatting.BOLD),
            x, y, color, true)
    }

    fun addDamage(amount: Float) {
        val target = BattleEnhanceMod.controlledPokemon ?: return
        damageNumbers.add(DamageData(target, amount, false, System.currentTimeMillis()))
    }

    fun addEnemyDamage(amount: Float) {
        val target = BattleEnhanceMod.battleTarget ?: return
        damageNumbers.add(DamageData(target, amount, false, System.currentTimeMillis()))
    }

    data class DamageData(val entity: LivingEntity, val amount: Float, val critical: Boolean, val createdAt: Long)
}
