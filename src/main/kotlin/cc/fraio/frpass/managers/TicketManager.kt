package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ItemBuilder
import cc.fraio.frpass.utils.msg
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class TicketManager(private val plugin: FrPass) {

    val ticketKey = NamespacedKey(plugin, "premium_ticket")

    fun getTicketItem(): ItemStack {
        val config = plugin.configManager.config
        val matStr = config.getString("premium-ticket.material", "PAPER") ?: "PAPER"
        val name = config.getString("premium-ticket.name", "&e&lPremium Pass Ticket") ?: "&e&lPremium Pass Ticket"
        val lore = config.getStringList("premium-ticket.lore")
        val modelData = config.getInt("premium-ticket.custom-model-data", 0)
        val glow = config.getBoolean("premium-ticket.glow", true)

        val item = ItemBuilder.fromIdentifier(matStr, Material.PAPER)
            .setName(name)
            .setLore(lore)
            .setCustomModelData(modelData)
            .setGlow(glow)
            .build()

        val meta = item.itemMeta
        if (meta != null) {
            meta.persistentDataContainer.set(ticketKey, PersistentDataType.BYTE, 1.toByte())
            item.itemMeta = meta
        }

        return item
    }

    fun giveTicket(player: Player, amount: Int = 1) {
        if (amount <= 0) return
        val ticket = getTicketItem()
        val maxStack = ticket.maxStackSize.coerceAtLeast(1)

        var remaining = amount
        var given = 0
        var pending = 0

        while (remaining > 0) {
            val toAdd = remaining.coerceAtMost(maxStack)
            val stack = ticket.clone().apply { this.amount = toAdd }
            val leftovers = player.inventory.addItem(stack)
            if (leftovers.isEmpty()) {
                given += toAdd
                remaining -= toAdd
            } else {
                val notAdded = leftovers.values.sumOf { it.amount }
                val successfullyAdded = toAdd - notAdded
                given += successfullyAdded
                remaining -= successfullyAdded
                pending += remaining
                break
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
        val maxStack = ticket.maxStackSize.coerceAtLeast(1)
        var totalGiven = 0

        while (pd.pendingTickets > 0) {
            val toAdd = pd.pendingTickets.coerceAtMost(maxStack)
            val stack = ticket.clone().apply { this.amount = toAdd }
            val leftovers = player.inventory.addItem(stack)
            if (leftovers.isEmpty()) {
                totalGiven += toAdd
                pd.pendingTickets -= toAdd
            } else {
                val notAdded = leftovers.values.sumOf { it.amount }
                val successfullyAdded = toAdd - notAdded
                if (successfullyAdded > 0) {
                    totalGiven += successfullyAdded
                    pd.pendingTickets -= successfullyAdded
                }
                break
            }
        }

        if (totalGiven > 0) {
            plugin.playerDataManager.savePlayer(player.uniqueId)
            player.sendMessage(player.msg("messages.ticket-dequeued", "%amount%" to totalGiven.toString()))
        }
    }
}
