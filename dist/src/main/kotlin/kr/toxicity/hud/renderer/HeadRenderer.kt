package kr.toxicity.hud.renderer

import kr.toxicity.hud.api.component.PixelComponent
import kr.toxicity.hud.api.component.WidthComponent
import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.api.player.HudPlayerHead
import kr.toxicity.hud.api.update.UpdateEvent
import kr.toxicity.hud.layout.LayoutAlign
import kr.toxicity.hud.manager.PlaceholderManagerImpl
import kr.toxicity.hud.manager.PlayerHeadManager
import kr.toxicity.hud.manager.PlayerManager
import kr.toxicity.hud.placeholder.ConditionBuilder
import kr.toxicity.hud.util.*
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import java.util.UUID

class HeadRenderer(
    private val components: List<TextComponent.Builder>,
    private val pixel: Int,
    private val x: Int,
    private val align: LayoutAlign,
    follow: String?,
    private val conditions: ConditionBuilder,
) {
    private val followPlayer = follow?.let {
        PlaceholderManagerImpl.find(it).apply {
            if (!java.lang.String::class.java.isAssignableFrom(clazz)) throw RuntimeException("This placeholder is not a string: $it")
        }
    }
    private val componentMap = HashMap<UUID, PixelComponent>()
    private val nextPixel = (-pixel * 8).toSpaceComponent() + NEGATIVE_ONE_SPACE_COMPONENT

    fun getHead(event: UpdateEvent): (HudPlayer) -> PixelComponent {
        val cond = conditions.build(event)
        val playerPlaceholder = followPlayer?.build(event)
        return build@{ player ->
            var targetPlayer = player
            var targetPlayerHead: HudPlayerHead? = null
            playerPlaceholder?.let {
                val value = it.value(player)
                val pair: Pair<HudPlayer, HudPlayerHead> = getHead(value, player) ?: return@build EMPTY_PIXEL_COMPONENT
                targetPlayer = pair.first
                targetPlayerHead = pair.second
            }
            if (targetPlayerHead == null) {
                println("targetPlayerHead is null")
                return@build EMPTY_PIXEL_COMPONENT
            }
            if (cond(targetPlayer)) synchronized(componentMap) {
                componentMap.computeIfAbsent(targetPlayer.bukkitPlayer.uniqueId) {
                    var comp = EMPTY_WIDTH_COMPONENT
                    targetPlayerHead!!.colors.forEachIndexed { index, textColor ->
                        comp += WidthComponent(components[index / 8].color(textColor), pixel)
                        comp += if (index < 63 && index % 8 == 7) nextPixel else NEGATIVE_ONE_SPACE_COMPONENT
                    }
                    comp.toPixelComponent(
                        when (align) {
                            LayoutAlign.LEFT -> x
                            LayoutAlign.CENTER -> x - comp.width / 2
                            LayoutAlign.RIGHT -> x - comp.width
                        }
                    )
                }
            } else EMPTY_PIXEL_COMPONENT
        }
    }

    private fun getHead(placeholderValue: Any, player: HudPlayer): Pair<HudPlayer, HudPlayerHead>? {
        if (placeholderValue is String) {
            val stringValue = placeholderValue.replace("%", "")
            if (stringValue.startsWith("skin:")) {
                val skin = stringValue.substring(5)
                return Pair(player, PlayerHeadManager.provideHead(skin))
            } else {
                val bukkitPlayer = Bukkit.getPlayer(stringValue)
                if (bukkitPlayer != null) {
                    return Pair(
                        PlayerManager.getHudPlayer(bukkitPlayer),
                        PlayerHeadManager.provideHead(bukkitPlayer.name)
                    )
                }
            }
        }
        println("Invalid placeholder value: $placeholderValue")
        return null
    }
}