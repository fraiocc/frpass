package cc.fraio.frpass.listeners

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.api.FrPassProvider
import cc.fraio.frpass.utils.ColorUtils
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class TicketListener(private val plugin: FrPass) : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        
        val player = event.player
        val item = event.item ?: return
        
        if (item.hasItemMeta()) {
            val meta = item.itemMeta!!
            if (meta.persistentDataContainer.has(plugin.ticketManager.ticketKey, PersistentDataType.BYTE)) {
                event.isCancelled = true
                
                val pd = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
                
                if (pd.premium) {
                    player.sendMessage(plugin.langManager.getMessage(player, "messages.already-premium"))
                    return
                }
                
                // Consume 1 item
                item.amount = item.amount - 1
                
                // Grant Premium
                FrPassProvider.get().setPremium(player.uniqueId, true)
                
                // Effects
                val soundName = plugin.configManager.config.getString("settings.sounds.level-up", "ENTITY_PLAYER_LEVELUP")
                try {
                    val sound = Sound.valueOf(soundName!!.uppercase())
                    player.playSound(player.location, sound, 1f, 1f)
                } catch (ignored: Exception) {}
                
                player.sendMessage(plugin.langManager.getMessage(player, "messages.ticket-used"))
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        plugin.foliaLib.impl.runLater({ task ->
            plugin.ticketManager.processPendingTickets(player)
        }, 10L) // Wait half a second so inventory actually closes and updates
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        plugin.foliaLib.impl.runLater({ task ->
            plugin.ticketManager.processPendingTickets(player)
        }, 60L) // Wait 3 seconds after join
    }
}
