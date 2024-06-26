package kr.toxicity.hud.manager

import kr.toxicity.hud.image.HudImage
import kr.toxicity.hud.image.ImageType
import kr.toxicity.hud.image.NamedLoadedImage
import kr.toxicity.hud.image.SplitType
import kr.toxicity.hud.pack.PackGenerator
import kr.toxicity.hud.resource.GlobalResource
import kr.toxicity.hud.util.*
import org.bukkit.configuration.MemoryConfiguration
import java.io.File

object ImageManager: BetterHudManager {

    private val imageMap = HashMap<String, HudImage>()
    private val emptySetting = MemoryConfiguration()

    override fun start() {
    }

    fun getImage(name: String) = synchronized(imageMap) {
        imageMap[name]
    }



    override fun reload(resource: GlobalResource, callback: () -> Unit) {
        synchronized(imageMap) {
            imageMap.clear()
        }
        val assets = DATA_FOLDER.subFolder("assets")
        DATA_FOLDER.subFolder("images").forEachAllYamlAsync({ file, s, configurationSection ->
            runCatching {
                val image = when (val type = ImageType.valueOf(configurationSection.getString("type").ifNull("type value not set.").uppercase())) {
                    ImageType.SINGLE -> {
                        HudImage(
                            file.path,
                            s,
                            listOf(NamedLoadedImage(
                                "$s.png",
                                File(assets, configurationSection.getString("file").ifNull("file value not set.").replace('/', File.separatorChar))
                                    .toImage()
                                    .removeEmptySide()
                                    .ifNull("Invalid image."),
                            )),
                            type,
                            configurationSection.getConfigurationSection("setting") ?: emptySetting
                        )
                    }
                    ImageType.LISTENER -> {
                        val splitType = (configurationSection.getString("split-type")?.let { splitType ->
                            runCatching {
                                SplitType.valueOf(splitType.uppercase())
                            }.getOrNull()
                        } ?: SplitType.LEFT)
                        HudImage(
                            file.path,
                            s,
                            splitType.split(s, File(assets, configurationSection.getString("file").ifNull("file value not set.").replace('/', File.separatorChar))
                                .toImage()
                                .removeEmptySide()
                                .ifNull("Invalid image.").image, configurationSection.getInt("split", 25).coerceAtLeast(1)),
                            type,
                            configurationSection.getConfigurationSection("setting").ifNull("setting configuration not found.")
                        )
                    }
                    ImageType.SEQUENCE -> {
                        HudImage(
                            file.path,
                            s,
                            configurationSection.getStringList("files").ifEmpty {
                                warn("files is empty.")
                                return@forEachAllYamlAsync
                            }.mapIndexed { index, string ->
                                File(assets, string.replace('/', File.separatorChar))
                                    .toImage()
                                    .removeEmptyWidth()
                                    .ifNull("Invalid image: $string")
                                    .toNamed("${s}_${index + 1}.png")
                            },
                            type,
                            configurationSection.getConfigurationSection("setting") ?: emptySetting
                        )
                    }
                }
                imageMap.putSync("image", s) {
                    image
                }
            }.onFailure { e ->
                warn(
                    "Unable to load this image: $s in ${file.name}",
                    "Reason: ${e.message}"
                )
            }
        }) {
            imageMap.values.forEach { value ->
                val list = value.image
                if (list.isNotEmpty()) {
                    list.forEach {
                        val file = ArrayList(resource.textures).apply {
                            add(it.name.substringBefore('.'))
                            add(it.name)
                        }
                        PackGenerator.addTask(file) {
                            it.image.image.toByteArray()
                        }
                    }
                }
            }
            callback()
        }
    }

    override fun end() {
    }
}