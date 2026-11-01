package cc.fraio.frpass.api.events

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class FrPassTierClaimEvent(
    val player: Player,
    val tierLevel: Int,
    val isPremiumReward: Boolean,
    isAsync: Boolean = !Bukkit.isPrimaryThread()
) : Event(isAsync), Cancellable {

    private var isCancelled = false

    override fun isCancelled(): Boolean = isCancelled

    override fun setCancelled(cancel: Boolean) {
        this.isCancelled = cancel
    }

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
