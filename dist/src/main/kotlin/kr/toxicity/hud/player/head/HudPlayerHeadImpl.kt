package kr.toxicity.hud.player.head

import com.google.gson.JsonParser
import kr.toxicity.hud.api.player.HudPlayerHead
import kr.toxicity.hud.manager.PlayerHeadManager
import kr.toxicity.hud.util.toImage
import kr.toxicity.hud.util.warn
import net.jodah.expiringmap.ExpiringMap
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.*

class HudPlayerHeadImpl(playerName: String) : HudPlayerHead {

    companion object {
        private val allBlack = (0..63).map {
            NamedTextColor.BLACK
        }
        private val expiringMap = ExpiringMap.builder()
            .expiration(5, java.util.concurrent.TimeUnit.MINUTES)
            .build<String, List<TextColor>>()
    }

    private val colorList = runCatching {
        expiringMap[playerName] ?: HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create(JsonParser.parseString(String(Base64.getDecoder().decode(PlayerHeadManager.provideSkin(playerName))))
                            .asJsonObject
                            .getAsJsonObject("textures")
                            .getAsJsonObject("SKIN")
                            .getAsJsonPrimitive("url")
                            .asString
                    )
                )
                .GET()
                .build(),
            BodyHandlers.ofInputStream()
        ).body().buffered().use {
            val ready = it.toImage()
            val image = ready.getSubimage(8, 8, 8, 8)
            val layer = ready.getSubimage(40, 8, 8, 8)
            val colors = (0..63).map { i ->
                val layerColor = layer.getRGB(i % 8, i / 8)
                val imageColor = image.getRGB(i % 8, i / 8)
                TextColor.color(if (layerColor ushr 24 != 0) layerColor else imageColor)
            }
            expiringMap[playerName] = colors
            colors
        }
    }.getOrElse { e ->
        warn(
            "Unable to get ${playerName}'s head.",
            "Reason: ${e.message}"
        )
        allBlack
    }

    override fun getColors(): List<TextColor> = colorList
}