package cc.fraio.frpass.commands

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class AdminCommand(private val plugin: FrPass) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val prefix = plugin.configManager.config.getString("settings.prefix") ?: "&8[&bFrPass&8] "
        
        if (!sender.hasPermission("frpass.admin")) {
            sender.sendMessage(ColorUtils.colorize("$prefix&cNo permission."))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(plugin.langManager.getMessage(null, "messages.admin-usage"))
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                plugin.configManager.reload()
                plugin.tierManager.loadAll()
                plugin.langManager.load()
                sender.sendMessage(plugin.langManager.getMessage(null, "messages.plugin-reloaded"))
            }
            "addxp" -> {
                if (args.size < 3) {
                    sender.sendMessage(plugin.langManager.getMessage(null, "messages.admin-usage"))
                    return true
                }
                val target = org.bukkit.Bukkit.getPlayer(args[1])
                val amount = args[2].toIntOrNull()
                if (target == null) {
                    sender.sendMessage(plugin.langManager.getMessage(null, "messages.player-not-found"))
                    return true
                }
                if (amount == null) {
                    sender.sendMessage(plugin.langManager.getMessage(null, "messages.invalid-amount"))
                    return true
                }
                plugin.playerDataManager.addXp(target.uniqueId, amount)
                sender.sendMessage(plugin.langManager.getMessage(null, "messages.added-xp", "%amount%" to amount.toString(), "%player%" to target.name))
            }
            "setpremium" -> {
                if (args.size < 3) {
                    sender.sendMessage(plugin.langManager.getMessage(null, "messages.admin-usage"))
                    return true
                }
                val target = org.bukkit.Bukkit.getPlayer(args[1])
                val value = args[2].toBooleanStrictOrNull()
                if (target == null) {
                    sender.sendMessage(plugin.langManager.getMessage(null, "messages.player-not-found"))
                    return true
                }
                if (value == null) {
                    sender.sendMessage(plugin.langManager.getMessage(null, "messages.admin-usage"))
                    return true
                }
                val data = plugin.playerDataManager.getPlayer(target.uniqueId)
                if (data != null) {
                    data.premium = value
                    sender.sendMessage(plugin.langManager.getMessage(null, "messages.set-premium", "%player%" to target.name, "%state%" to value.toString()))
                } else {
                    sender.sendMessage(plugin.langManager.getMessage(null, "messages.player-not-found"))
                }
            }
            else -> {
                sender.sendMessage(ColorUtils.colorize("$prefix&cUnknown command."))
            }
        }
        
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (!sender.hasPermission("frpass.admin")) return emptyList()
        
        if (args.size == 1) {
            val subCommands = listOf("reload", "addxp", "setpremium")
            return subCommands.filter { it.startsWith(args[0], ignoreCase = true) }
        } else if (args.size == 2) {
            val cmd = args[0].lowercase()
            if (cmd == "addxp" || cmd == "setpremium") {
                return org.bukkit.Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
            }
        } else if (args.size == 3) {
            val cmd = args[0].lowercase()
            if (cmd == "addxp") {
                if (args[2].isEmpty()) return listOf("<amount>")
            } else if (cmd == "setpremium") {
                return listOf("true", "false").filter { it.startsWith(args[2], ignoreCase = true) }
            }
        }
        
        return emptyList()
    }
}
