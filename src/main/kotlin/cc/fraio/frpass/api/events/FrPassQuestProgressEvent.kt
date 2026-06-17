package cc.fraio.frpass.api.events

import cc.fraio.frpass.quests.Quest
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class FrPassQuestProgressEvent(
    val player: Player,
    val quest: Quest,
    val oldProgress: Int,
    val newProgress: Int,
    val isCompleted: Boolean
) : Event() {

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    companion object {
        private val HANDLERS = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }
}
