package cc.fraio.frpass.listeners

import cc.fraio.frpass.FrPass
import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MenuPacketListener(private val plugin: FrPass) : PacketListener {
    
    private val lastClick = ConcurrentHashMap<UUID, Long>()

    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packetType == PacketType.Play.Client.CLICK_WINDOW) {
            val player = event.getPlayer<Player>() ?: return
            
            // PacketEvents fires asynchronously, so we use ConcurrentHashMap to ensure thread safety
            if (plugin.menuManager.isPluginMenu(player)) {
                val now = System.currentTimeMillis()
                val last = lastClick[player.uniqueId] ?: 0L
                
                // Anti-Spam / Auto-Clicker Prevention (50ms cooldown)
                if (now - last < 50) {
                    event.isCancelled = true
                    return
                }
                lastClick[player.uniqueId] = now
            }
        }
    }
}
