package cc.fraio.frpass.commands

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.hooks.VaultHook
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class PassCommand(private val plugin: FrPass) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(plugin.langManager.getMessage(null, "messages.only-players"))
            return true
        }

        if (args.isNotEmpty() && args[0].equals("gift", ignoreCase = true)) {
            handleGift(sender, args)
            return true
        }

        plugin.menuManager.openMainMenu(sender)
        return true
    }

    private fun handleGift(sender: Player, args: Array<out String>) {
        val config = plugin.configManager.config
        
        if (!config.getBoolean("gift.enabled", true)) {
            sender.sendMessage(plugin.langManager.getMessage(sender, "messages.gift-disabled"))
            return
        }

        val perm = config.getString("gift.permission", "frpass.gift") ?: "frpass.gift"
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(plugin.langManager.getMessage(sender, "messages.gift-no-permission"))
            return
        }

        if (args.size < 2) {
            sender.sendMessage(plugin.langManager.getMessage(sender, "messages.admin-usage"))
            sender.sendMessage("§cUsage: /frpass gift <player>")
            return
        }

        val targetName = args[1]
        val target = Bukkit.getPlayer(targetName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(plugin.langManager.getMessage(sender, "messages.player-not-found"))
            return
        }

        if (target.uniqueId == sender.uniqueId) {
            sender.sendMessage(plugin.langManager.getMessage(sender, "messages.gift-cannot-gift-self"))
            return
        }

        val senderData = plugin.playerDataManager.getPlayer(sender.uniqueId)
        if (senderData == null || !senderData.premium) {
            sender.sendMessage(plugin.langManager.getMessage(sender, "messages.gift-not-premium"))
            return
        }

        val targetData = plugin.playerDataManager.getPlayer(target.uniqueId)
        if (targetData == null) {
            sender.sendMessage(plugin.langManager.getMessage(sender, "messages.player-not-found"))
            return
        }

        if (targetData.premium) {
            sender.sendMessage(plugin.langManager.getMessage(sender, "messages.gift-target-already-premium", "%player%" to target.name))
            return
        }

        val minLevel = config.getInt("gift.cost.level", 0)
        if (senderData.level < minLevel) {
            sender.sendMessage(plugin.langManager.getMessage(sender, "messages.gift-not-enough-level", "%level%" to minLevel.toString()))
            return
        }

        val requiredXp = config.getInt("gift.cost.xp", 0)
        if (senderData.xp < requiredXp) {
            sender.sendMessage(plugin.langManager.getMessage(sender, "messages.gift-not-enough-xp", "%amount%" to requiredXp.toString()))
            return
        }

        val requiredMoney = config.getDouble("gift.cost.money", 0.0)
        if (requiredMoney > 0.0) {
            val eco = VaultHook.economy
            if (eco != null) {
                if (!eco.has(sender, requiredMoney)) {
                    sender.sendMessage(plugin.langManager.getMessage(sender, "messages.gift-not-enough-money", "%amount%" to requiredMoney.toString()))
                    return
                }
            }
        }

        if (requiredMoney > 0.0) {
            VaultHook.economy?.withdrawPlayer(sender, requiredMoney)
        }
        if (requiredXp > 0) {
            senderData.xp -= requiredXp
        }

        val transferPremium = config.getBoolean("gift.transfer-premium", true)
        if (transferPremium) {
            senderData.premium = false
            plugin.playerDataManager.savePlayer(sender.uniqueId)
        }

        targetData.premium = true
        plugin.playerDataManager.savePlayer(target.uniqueId)

        sender.sendMessage(plugin.langManager.getMessage(sender, "messages.gift-sent", "%player%" to target.name))
        target.sendMessage(plugin.langManager.getMessage(target, "messages.gift-received", "%player%" to sender.name))
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return listOf("gift").filter { it.startsWith(args[0], ignoreCase = true) }
        }
        if (args.size == 2 && args[0].equals("gift", ignoreCase = true)) {
            return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
        }
        return emptyList()
    }
}
