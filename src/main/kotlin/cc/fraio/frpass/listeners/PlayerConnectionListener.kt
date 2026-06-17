package cc.fraio.frpass.listeners

import cc.fraio.frpass.FrPass
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
            player.sendMessage(cc.fraio.frpass.utils.ColorUtils.colorize("&8[&bFrPass&8] &aNew Update!! Download: &n$link"))
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.playerDataManager.unloadPlayer(event.player.uniqueId)
        plugin.questTasks.cleanPlayer(event.player.uniqueId)
    }
}
