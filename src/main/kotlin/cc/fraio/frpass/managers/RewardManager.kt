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
                    "COMMAND" -> executeCommandReward(player, content)
                    "MESSAGE" -> executeMessageReward(player, content)
                    "MONEY" -> executeMoneyReward(player, content)
                    "XP" -> executeXpReward(player, content)
                    "ITEM" -> executeItemReward(player, content)
                    else -> plugin.logger.warning("Unknown reward type '$type' in reward: '$reward'")
                }
            }
        }
    }

    private fun executeCommandReward(player: Player, content: String) {
        val cmd = content.replace("%player%", player.name)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
    }

    private fun executeMessageReward(player: Player, content: String) {
        val msg = content.replace("%player%", player.name)
        player.sendMessage(ColorUtils.colorize(player, msg))
    }

    private fun executeMoneyReward(player: Player, content: String) {
        val amount = content.toDoubleOrNull()
        if (amount != null && VaultHook.economy != null) {
            VaultHook.economy?.depositPlayer(player, amount)
        } else if (VaultHook.economy == null) {
            plugin.logger.warning("Vault economy is not hooked, but [MONEY] reward was triggered ($content).")
        }
    }

    private fun executeXpReward(player: Player, content: String) {
        val amount = content.toIntOrNull()
        if (amount != null) {
            player.giveExp(amount)
        }
    }

    // Format: DIAMOND:64 name:"&bSpecial Diamond" lore:"&7Line 1|&7Line 2" custom-model-data:123
    // Alternative format (underscores): DIAMOND:64 name:&bSpecial_Diamond lore:&7Line_1|&7Line_2
    private fun executeItemReward(player: Player, content: String) {
        val tokens = parseRewardTokens(content)
        if (tokens.isEmpty()) return

        val matAmount = tokens[0].split(":")
        val materialStr = matAmount[0]
        val amount = if (matAmount.size > 1) matAmount[1].toIntOrNull() ?: 1 else 1
        val material = Material.matchMaterial(materialStr) ?: return

        val builder = ItemBuilder(material)
        
        for (i in 1 until tokens.size) {
            val token = tokens[i]
            when {
                token.startsWith("name:", ignoreCase = true) -> {
                    val name = token.substring(5).removeSurrounding("\"").replace("_", " ")
                    builder.setName(name, player)
                }
                token.startsWith("lore:", ignoreCase = true) -> {
                    val rawLore = token.substring(5).removeSurrounding("\"")
                    val loreLines = rawLore.split("|").map { it.replace("_", " ") }
                    builder.setLore(loreLines, player)
                }
                token.startsWith("custom-model-data:", ignoreCase = true) -> {
                    val modelData = token.substring(18).toIntOrNull() ?: 0
                    builder.setCustomModelData(modelData)
                }
            }
        }

        val item = builder.build()
        item.amount = amount

        val leftover = player.inventory.addItem(item)
        if (leftover.isNotEmpty()) {
            for (dropItem in leftover.values) {
                player.world.dropItemNaturally(player.location, dropItem)
            }
        }
    }

    private fun parseRewardTokens(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in input) {
            if (ch == '"') {
                inQuotes = !inQuotes
                sb.append(ch)
            } else if (ch == ' ' && !inQuotes) {
                if (sb.isNotEmpty()) {
                    tokens.add(sb.toString())
                    sb.clear()
                }
            } else {
                sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) {
            tokens.add(sb.toString())
        }
        return tokens
    }
}
