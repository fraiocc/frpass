package cc.fraio.frpass.commands

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.msg
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class AdminCommand(private val plugin: FrPass) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("frpass.admin")) {
            val prefix = plugin.configManager.config.getString("settings.prefix", "&8[&bFrPass&8] ") ?: "&8[&bFrPass&8] "
            sender.sendMessage(ColorUtils.colorize("$prefix&cNo permission."))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(sender.msg("messages.admin-usage"))
            return true
        }

        when (args[0].lowercase()) {
            "addxp" -> {
                if (args.size < 3) {
                    sender.sendMessage(ColorUtils.colorize("&cUsage: /frpassadmin addxp <player> <amount>"))
                    return true
                }
                val target = org.bukkit.Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage(sender.msg("messages.player-not-found"))
                    return true
                }
                val amount = args[2].toIntOrNull()
                if (amount == null) {
                    sender.sendMessage(sender.msg("messages.invalid-amount"))
                    return true
                }
                plugin.playerDataManager.addXp(target.uniqueId, amount)
                sender.sendMessage(sender.msg("messages.added-xp", "%amount%" to amount.toString(), "%player%" to target.name))
            }
            "setpremium" -> {
                if (args.size < 3) {
                    sender.sendMessage(ColorUtils.colorize("&cUsage: /frpassadmin setpremium <player> <true|false>"))
                    return true
                }
                val target = org.bukkit.Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage(sender.msg("messages.player-not-found"))
                    return true
                }
                val value = args[2].toBooleanStrictOrNull()
                if (value == null) {
                    sender.sendMessage(ColorUtils.colorize("&cUsage: /frpassadmin setpremium <player> <true|false>"))
                    return true
                }
                val data = plugin.playerDataManager.getPlayer(target.uniqueId)
                if (data != null) {
                    data.premium = value
                    plugin.playerDataManager.savePlayer(target.uniqueId)
                    sender.sendMessage(sender.msg("messages.set-premium", "%player%" to target.name, "%state%" to value.toString()))
                } else {
                    sender.sendMessage(sender.msg("messages.player-not-found"))
                }
            }
            "giveticket" -> {
                if (args.size < 3) {
                    sender.sendMessage(ColorUtils.colorize("&cUsage: /frpassadmin giveticket <player> <amount>"))
                    return true
                }
                val target = org.bukkit.Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage(sender.msg("messages.player-not-found"))
                    return true
                }
                val amount = args[2].toIntOrNull()
                if (amount == null || amount <= 0) {
                    sender.sendMessage(sender.msg("messages.invalid-amount"))
                    return true
                }
                plugin.ticketManager.giveTicket(target, amount)
                sender.sendMessage(sender.msg("messages.given-ticket", "%amount%" to amount.toString(), "%player%" to target.name))
            }
            "editor" -> {
                if (sender !is org.bukkit.entity.Player) {
                    sender.sendMessage(sender.msg("messages.only-players"))
                    return true
                }
                plugin.questEditorManager.openQuestListMenu(sender, 1)
            }
            else -> {
                sender.sendMessage(sender.msg("messages.admin-usage"))
            }
        }
        
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (!sender.hasPermission("frpass.admin")) return emptyList()
        
        if (args.size == 1) {
            val subCommands = listOf("addxp", "setpremium", "giveticket", "editor")
            return subCommands.filter { it.startsWith(args[0], ignoreCase = true) }
        } else if (args.size == 2) {
            val cmd = args[0].lowercase()
            if (cmd == "addxp" || cmd == "setpremium" || cmd == "giveticket") {
                return org.bukkit.Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
            }
        } else if (args.size == 3) {
            val cmd = args[0].lowercase()
            if (cmd == "addxp" || cmd == "giveticket") {
                if (args[2].isEmpty()) return listOf("<amount>")
            } else if (cmd == "setpremium") {
                return listOf("true", "false").filter { it.startsWith(args[2], ignoreCase = true) }
            }
        }
        
        return emptyList()
    }
}
