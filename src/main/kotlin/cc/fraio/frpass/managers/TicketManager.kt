package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType

class TicketManager(private val plugin: FrPass) {

    val ticketKey = NamespacedKey(plugin, "premium_ticket")

    fun getTicketItem(): ItemStack {
        val config = plugin.configManager.config
        val materialStr = config.getString("premium-ticket.material", "PAPER") ?: "PAPER"
        val material = Material.getMaterial(materialStr.uppercase()) ?: Material.PAPER
        
        val name = config.getString("premium-ticket.name", "&e&lPremium Pass Ticket") ?: "&e&lPremium Pass Ticket"
        val lore = config.getStringList("premium-ticket.lore")
        val cmd = config.getInt("premium-ticket.custom-model-data", 0)
        val glow = config.getBoolean("premium-ticket.glow", true)

        val item = ItemBuilder(material)
            .setName(name)
            .setLore(lore)
            .setCustomModelData(cmd)
            .setGlow(glow)
            .build()
            
        val meta = item.itemMeta
        meta?.persistentDataContainer?.set(ticketKey, PersistentDataType.BYTE, 1.toByte())
        item.itemMeta = meta
        
        return item
    }

    fun giveTicket(player: Player, amount: Int) {
        val ticket = getTicketItem()
        ticket.amount = 1
        
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
            player.sendMessage(plugin.langManager.getMessage(player, "messages.ticket-received", "%amount%" to given.toString()))
        }
        
        if (pending > 0) {
            val pd = plugin.playerDataManager.getPlayer(player.uniqueId)
            if (pd != null) {
                pd.pendingTickets += pending
                plugin.playerDataManager.savePlayer(player.uniqueId)
            }
            player.sendMessage(plugin.langManager.getMessage(player, "messages.ticket-queued", "%amount%" to pending.toString()))
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
            player.sendMessage(plugin.langManager.getMessage(player, "messages.ticket-dequeued", "%amount%" to given.toString()))
        }
    }
}
