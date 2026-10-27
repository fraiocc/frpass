package cc.fraio.frpass.listeners

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.msg
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerConnectionListener(private val plugin: FrPass) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        plugin.playerDataManager.loadPlayer(player.uniqueId)
        
        if (player.isOp && plugin.updateChecker.updateAvailable) {
            val link = plugin.updateChecker.downloadLink
            player.sendMessage(player.msg("messages.update-available", "%link%" to link))
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.playerDataManager.unloadPlayer(event.player.uniqueId)
        plugin.questTasks.cleanPlayer(event.player.uniqueId)
        plugin.menuPacketListener.cleanPlayer(event.player.uniqueId)
    }
}
