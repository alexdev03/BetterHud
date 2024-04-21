package kr.toxicity.hud.manager

import kr.toxicity.hud.api.manager.PopupManager
import kr.toxicity.hud.api.popup.Popup
import kr.toxicity.hud.popup.PopupImpl
import kr.toxicity.hud.resource.GlobalResource
import kr.toxicity.hud.util.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object PopupManagerImpl: BetterHudManager, PopupManager {
    private val popupMap = HashMap<String, PopupImpl>()
    override fun start() {

    }

    override fun reload(resource: GlobalResource, callback: () -> Unit) {
        popupMap.clear()
        val save = ArrayList(resource.font).apply {
            add("popup")
        }
        DATA_FOLDER.subFolder("popups").forEachAllYamlAsync({ file, s, configurationSection ->
            runCatching {
                popupMap.putSync("popup", s) {
                    PopupImpl(file.path, save, s, configurationSection)
                }
            }.onFailure { e ->
                warn("Unable to load this popup: $s in ${file.name}")
                warn("Reason: ${e.message}")
            }
        }, callback)
    }

    override fun getAllNames(): MutableSet<String> = Collections.unmodifiableSet(popupMap.keys)
    override fun getPopup(name: String): Popup? = popupMap[name]
    override fun getDefaultPopups(): Set<Popup> = popupMap.values.filter {
        it.isDefault
    }.toSet()
    override fun end() {
    }
}