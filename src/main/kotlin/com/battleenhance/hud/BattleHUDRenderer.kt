package com.battleenhance.hud

import com.cobblemon.mod.common.api.events.battles.BattleStartedPostEvent
import com.cobblemon.mod.common.api.events.battles.BattleEndedEvent
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.entity.LivingEntity
import java.util.concurrent.ConcurrentLinkedQueue

object BattleHUDRenderer {
    private var isActive = false
    private val damageNumbers = ConcurrentLinkedQueue<DamageNumberData>()
    private val hpBars = mutableListOf<HPBarData>()

    private const val COLOR_HP_GREEN = 0xFF00
    private const val COLOR_HP_YELLOW = 0xFFFF00
    private const val COLOR_HP_RED = 0xFF0000
    private const val COLOR_HP_BG = 0x404040
    private const val COLOR_HP_BORDER = 0xFFFFFF
    private const val COLOR_DAMAGE = 0xFF4444
    private const val COLOR_HEAL = 0x44FF44

    fun register() {
        BattleStartedPostEvent.EVENT.register { event ->
            startHUD(event)
        }

        BattleEndedEvent.EVENT.register {
            endHUD()
        }

        HudRenderCallback.EVENT.register { context, tickCounter ->
            if (isActive) {
                render(context)
            }
        }
    }

    private fun startHUD(event: BattleStartedPostEvent) {
        isActive = true
        hpBars.clear()
        damageNumbers.clear()

        val battle = event.battle
        for (pokemon in battle.activePokemon) {
            val entity = pokemon.entity ?: continue
            val name = pokemon.name
            val hp = pokemon.currentHealth.toFloat()
            val maxHp = pokemon.maxHealth.toFloat()

            hpBars.add(HPBarData(
                entity = entity,
                name = name,
                health = hp,
                maxHealth = maxHp,
                isWild = entity.ownerUUID == null
            ))
        }
    }

    private fun endHUD() {
        isActive = false
        hpBars.clear()
        damageNumbers.clear()
    }

    private fun render(context: GuiGraphics) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val textRenderer = client.font
        val screenWidth = client.window.guiScaledWidth
        val screenHeight = client.window.guiScaledHeight

        for (bar in hpBars) {
            renderHPBar(context, bar, screenWidth, screenHeight)
        }

        val iterator = damageNumbers.iterator()
        while (iterator.hasNext()) {
            val num = iterator.next()
            val elapsed = System.currentTimeMillis() - num.createdAt
            if (elapsed > 2000) {
                iterator.remove()
                continue
            }
            renderDamageNumber(context, num, elapsed, screenWidth, screenHeight)
        }
    }

    private fun renderHPBar(context: GuiGraphics, bar: HPBarData, screenWidth: Int, screenHeight: Int) {
        val textRenderer = Minecraft.getInstance().font

        val barWidth = 120
        val barHeight = 8
        val x = (screenWidth - barWidth) / 2
        val y = 30

        context.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, COLOR_HP_BORDER)
        context.fill(x, y, x + barWidth, y + barHeight, COLOR_HP_BG)

        val hpPercent = bar.health / bar.maxHealth

        val hpColor = when {
            hpPercent > 0.5f -> COLOR_HP_GREEN
            hpPercent > 0.2f -> COLOR_HP_YELLOW
            else -> COLOR_HP_RED
        }

        val filledWidth = (barWidth * hpPercent).toInt()
        context.fill(x, y, x + filledWidth, y + barHeight, hpColor)

        val nameText = bar.name
        val hpText = "%.0f / %.0f".format(bar.health, bar.maxHealth)

        context.drawString(textRenderer, Component.literal(nameText), x, y - 12, 0xFFFFFF, true)
        context.drawString(textRenderer, Component.literal(hpText), x + barWidth + 5, y, 0xAAAAAA, true)

        if (bar.isWild) {
            context.drawString(textRenderer, Component.literal("(Wild)"), x, y + barHeight + 4, 0xAAAAAA, true)
        }
    }

    private fun renderDamageNumber(context: GuiGraphics, num: DamageNumberData, elapsed: Long, screenWidth: Int, screenHeight: Int) {
        val textRenderer = Minecraft.getInstance().font

        val screenX = screenWidth / 2 + ((Math.random() - 0.5) * 30).toInt()
        val screenY = screenHeight / 2 - 50 - (elapsed * 0.03).toInt()

        val text = if (num.amount > 0) "-%.0f".format(num.amount) else "+%.0f".format(-num.amount)
        val color = if (num.amount > 0) COLOR_DAMAGE else COLOR_HEAL

        val alpha = (1.0 - elapsed / 2000.0).coerceIn(0.0, 1.0)
        val drawColor = ((alpha * 255).toInt() shl 24) or (color and 0x00FFFFFF)

        context.drawString(
            textRenderer,
            Component.literal(text).withStyle(ChatFormatting.BOLD),
            screenX,
            screenY,
            drawColor,
            true
        )
    }

    fun addDamageNumber(entity: LivingEntity, amount: Float, isCritical: Boolean = false) {
        damageNumbers.add(DamageNumberData(
            entity = entity,
            amount = amount,
            isCritical = isCritical,
            createdAt = System.currentTimeMillis()
        ))
    }

    fun updateHP(entity: LivingEntity, newHealth: Float) {
        hpBars.find { it.entity == entity }?.let {
            it.health = newHealth
        }
    }

    data class HPBarData(
        val entity: LivingEntity,
        val name: Component,
        var health: Float,
        val maxHealth: Float,
        val isWild: Boolean
    )

    data class DamageNumberData(
        val entity: LivingEntity,
        val amount: Float,
        val isCritical: Boolean,
        val createdAt: Long
    )
}
