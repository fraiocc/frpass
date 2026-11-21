package cc.fraio.frpass.listeners

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.api.FrPassProvider
import cc.fraio.frpass.utils.SoundUtils
import cc.fraio.frpass.utils.sendMsg
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.persistence.PersistentDataType

class TicketListener(private val plugin: FrPass) : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        
        val player = event.player
        val item = event.item ?: return
        
        val meta = item.itemMeta ?: return
        if (!meta.persistentDataContainer.has(plugin.ticketManager.ticketKey, PersistentDataType.BYTE)) return

        event.isCancelled = true
        
        val pd = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
        if (pd.premium) {
            player.sendMsg("messages.already-premium")
            return
        }

        if (item.amount > 1) {
            item.amount -= 1
        } else {
            player.inventory.setItemInMainHand(null)
        }

        FrPassProvider.get().setPremium(player.uniqueId, true)

        val soundName = plugin.configManager.config.getString("settings.sounds.level-up", "ENTITY_PLAYER_LEVELUP")
        SoundUtils.play(player, soundName)

        player.sendMsg("messages.ticket-used")
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        plugin.foliaLib.scheduler.runAtEntityLater(player, Runnable {
            if (player.isOnline) {
                plugin.ticketManager.processPendingTickets(player)
            }
        }, 10L)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        plugin.foliaLib.scheduler.runAtEntityLater(player, Runnable {
            if (player.isOnline) {
                plugin.ticketManager.processPendingTickets(player)
            }
        }, 60L)
    }
}
