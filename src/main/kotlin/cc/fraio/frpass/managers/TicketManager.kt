package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.ItemBuilder
import cc.fraio.frpass.utils.msg
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType

class TicketManager(private val plugin: FrPass) {

    val ticketKey = NamespacedKey(plugin, "premium_ticket")

    fun getTicketItem(): ItemStack {
        val matStr = plugin.configManager.config.getString("ticket-item.material", "PAPER") ?: "PAPER"
        val mat = Material.matchMaterial(matStr) ?: Material.PAPER
        val name = plugin.configManager.config.getString("ticket-item.name", "&e&lPREMIUM BATTLEPASS TICKET") ?: "&e&lPREMIUM BATTLEPASS TICKET"
        val lore = plugin.configManager.config.getStringList("ticket-item.lore")
        val modelData = plugin.configManager.config.getInt("ticket-item.custom-model-data", 0)

        val item = ItemBuilder(mat)
            .setName(name)
            .setLore(lore)
            .setCustomModelData(modelData)
            .build()

        val meta = item.itemMeta
        if (meta != null) {
            meta.persistentDataContainer.set(ticketKey, PersistentDataType.BYTE, 1.toByte())
            item.itemMeta = meta
        }

        return item
    }

    fun giveTicket(player: Player, amount: Int = 1) {
        val ticket = getTicketItem()
        
        var given = 0
        var pending = 0
        
        for (i in 1..amount) {
            if (player.inventory.firstEmpty() == -1) {
                pending++
            } else {
                player.inventory.addItem(ticket.clone())
                given++
            }
        }
        
        if (given > 0) {
            player.sendMessage(player.msg("messages.ticket-received", "%amount%" to given.toString()))
        }
        
        if (pending > 0) {
            val pd = plugin.playerDataManager.getPlayer(player.uniqueId)
            if (pd != null) {
                pd.pendingTickets += pending
                plugin.playerDataManager.savePlayer(player.uniqueId)
            }
            player.sendMessage(player.msg("messages.ticket-queued", "%amount%" to pending.toString()))
        }
    }
    
    fun processPendingTickets(player: Player) {
        val pd = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
        if (pd.pendingTickets <= 0) return
        
        val ticket = getTicketItem()
        ticket.amount = 1
        
        var given = 0
        while (pd.pendingTickets > 0 && player.inventory.firstEmpty() != -1) {
            player.inventory.addItem(ticket.clone())
            pd.pendingTickets--
            given++
        }
        
        if (given > 0) {
            plugin.playerDataManager.savePlayer(player.uniqueId)
            player.sendMessage(player.msg("messages.ticket-dequeued", "%amount%" to given.toString()))
        }
    }
}
