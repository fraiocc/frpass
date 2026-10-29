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
        val player = event.player
        plugin.playerDataManager.unloadPlayer(player.uniqueId)
        plugin.questTasks.cleanPlayer(player.uniqueId)
        plugin.menuPacketListener?.cleanPlayer(player.uniqueId)
        plugin.menuManager.removeOpenMenu(player)
        plugin.menuManager.stopRerollSession(player)
        plugin.questEditorManager.closeEditorMenu(player)
        plugin.questEditorManager.endSession(player)
    }
}
