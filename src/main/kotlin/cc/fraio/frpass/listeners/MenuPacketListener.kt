package cc.fraio.frpass.listeners

import cc.fraio.frpass.FrPass
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketListenerCommon
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MenuPacketListener(private val plugin: FrPass) : PacketListener {
    
    private val lastClick = ConcurrentHashMap<UUID, Long>()
    private var handle: PacketListenerCommon? = null

    fun register() {
        handle = PacketEvents.getAPI().eventManager.registerListener(this, PacketListenerPriority.NORMAL)
    }

    fun unregister() {
        handle?.let {
            PacketEvents.getAPI().eventManager.unregisterListener(it)
            handle = null
        }
    }

    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packetType == PacketType.Play.Client.CLICK_WINDOW) {
            val player = event.getPlayer<Player>() ?: return
            
            if (plugin.menuManager.isPluginMenu(player)) {
                val now = System.currentTimeMillis()
                val last = lastClick[player.uniqueId] ?: 0L
                
                if (now - last < 50) {
                    event.isCancelled = true
                    return
                }
                lastClick[player.uniqueId] = now
            }
        }
    }

    fun cleanPlayer(uuid: UUID) {
        lastClick.remove(uuid)
    }
}
