package cc.fraio.frpass.listeners

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.sendMsg
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
            player.sendMsg("messages.update-available", "%link%" to link)
        }

        if (plugin.configManager.config.getBoolean("settings.notify-unclaimed-on-join", true)) {
            plugin.foliaLib.scheduler.runAtEntityLater(player, Runnable {
                if (!player.isOnline) return@Runnable
                val unclaimed = plugin.playerDataManager.getUnclaimedRewardCount(player.uniqueId)
                if (unclaimed > 0) {
                    player.sendMsg("messages.unclaimed-rewards-reminder", "%amount%" to unclaimed.toString())
                }
            }, 60L)
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
        plugin.editorManager.closeEditorMenu(player)
        plugin.editorManager.endSession(player)
    }
}
