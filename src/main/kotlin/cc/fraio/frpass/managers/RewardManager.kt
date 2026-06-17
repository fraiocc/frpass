package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.hooks.VaultHook
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

class RewardManager(private val plugin: FrPass) {

    fun giveRewards(player: Player, rewards: List<String>) {
        for (reward in rewards) {
            val typeEnd = reward.indexOf(']')
            if (reward.startsWith("[") && typeEnd != -1) {
                val type = reward.substring(1, typeEnd).uppercase()
                val content = reward.substring(typeEnd + 1).trim()
                
                when (type) {
                    "COMMAND" -> {
                        val cmd = content.replace("%player%", player.name)
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                    }
                    "MESSAGE" -> {
                        player.sendMessage(ColorUtils.colorize(player, content.replace("%player%", player.name)))
                    }
                    "MONEY" -> {
                        val amount = content.toDoubleOrNull()
                        if (amount != null && VaultHook.economy != null) {
                            VaultHook.economy?.depositPlayer(player, amount)
                        } else if (VaultHook.economy == null) {
                            plugin.logger.warning("Vault is not hooked, but MONEY reward was triggered.")
                        }
                    }
                    "XP" -> {
                        val amount = content.toIntOrNull()
                        if (amount != null) {
                            player.giveExp(amount)
                        }
                    }
                    "ITEM" -> {
                        // Example: DIAMOND:64 name:&bDiamond
                        val parts = content.split(" ")
                        if (parts.isNotEmpty()) {
                            val matAmount = parts[0].split(":")
                            val materialStr = matAmount[0]
                            val amount = if (matAmount.size > 1) matAmount[1].toIntOrNull() ?: 1 else 1
                            
                            val material = Material.matchMaterial(materialStr) ?: continue
                            val builder = ItemBuilder(material)
                            val item = builder.build()
                            item.amount = amount
                            
                            // A very basic parser for name, we can expand later
                            for (i in 1 until parts.size) {
                                if (parts[i].startsWith("name:")) {
                                    val name = parts[i].substring(5).replace("_", " ")
                                    val meta = item.itemMeta
                                    meta?.setDisplayName(ColorUtils.colorize(player, name))
                                    item.itemMeta = meta
                                }
                            }
                            player.inventory.addItem(item)
                        }
                    }
                }
            }
        }
    }
}
