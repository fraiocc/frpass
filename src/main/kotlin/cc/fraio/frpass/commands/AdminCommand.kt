package cc.fraio.frpass.commands

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.msg
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class AdminCommand(private val plugin: FrPass) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("frpass.admin")) {
            sender.sendMessage(sender.msg("messages.no-permission"))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(sender.msg("messages.admin-usage"))
            return true
        }

        when (args[0].lowercase()) {
            "addxp" -> {
                if (args.size < 3) {
                    sender.sendMessage(sender.msg("messages.usage-addxp"))
                    return true
                }
                val target = requireOnlinePlayer(sender, args[1]) ?: return true
                val amount = args[2].toIntOrNull()
                if (amount == null || amount <= 0) {
                    sender.sendMessage(sender.msg("messages.invalid-amount"))
                    return true
                }
                plugin.playerDataManager.addXp(target.uniqueId, amount)
                sender.sendMessage(sender.msg("messages.added-xp", "%amount%" to amount.toString(), "%player%" to target.name))
            }
            "setxp" -> {
                if (args.size < 3) {
                    sender.sendMessage(sender.msg("messages.usage-setxp"))
                    return true
                }
                val target = requireOnlinePlayer(sender, args[1]) ?: return true
                val amount = args[2].toIntOrNull()
                if (amount == null || amount < 0) {
                    sender.sendMessage(sender.msg("messages.invalid-amount"))
                    return true
                }
                plugin.playerDataManager.setXp(target.uniqueId, amount)
                sender.sendMessage(sender.msg("messages.set-xp", "%amount%" to amount.toString(), "%player%" to target.name))
            }
            "setlevel" -> {
                if (args.size < 3) {
                    sender.sendMessage(sender.msg("messages.usage-setlevel"))
                    return true
                }
                val target = requireOnlinePlayer(sender, args[1]) ?: return true
                val level = args[2].toIntOrNull()
                if (level == null || level < 0) {
                    sender.sendMessage(sender.msg("messages.invalid-amount"))
                    return true
                }
                plugin.playerDataManager.setLevel(target.uniqueId, level)
                sender.sendMessage(sender.msg("messages.set-level", "%level%" to level.toString(), "%player%" to target.name))
            }
            "reset" -> {
                if (args.size < 2) {
                    sender.sendMessage(sender.msg("messages.usage-reset"))
                    return true
                }
                val target = requireOnlinePlayer(sender, args[1]) ?: return true
                plugin.playerDataManager.resetPlayer(target.uniqueId)
                sender.sendMessage(sender.msg("messages.reset-player", "%player%" to target.name))
            }
            "completequest" -> {
                if (args.size < 3) {
                    sender.sendMessage(sender.msg("messages.usage-completequest"))
                    return true
                }
                val target = requireOnlinePlayer(sender, args[1]) ?: return true
                val questId = args[2]
                if (!plugin.questManager.quests.containsKey(questId)) {
                    sender.sendMessage(sender.msg("messages.quest-not-found", "%quest%" to questId))
                    return true
                }
                val completed = plugin.questManager.completeQuest(target, questId)
                if (completed) {
                    sender.sendMessage(sender.msg("messages.completed-quest", "%quest%" to questId, "%player%" to target.name))
                } else {
                    sender.sendMessage(sender.msg("messages.quest-already-completed", "%quest%" to questId, "%player%" to target.name))
                }
            }
            "setpremium" -> {
                if (args.size < 3) {
                    sender.sendMessage(sender.msg("messages.usage-setpremium"))
                    return true
                }
                val target = requireOnlinePlayer(sender, args[1]) ?: return true
                val value = args[2].toBooleanStrictOrNull()
                if (value == null) {
                    sender.sendMessage(sender.msg("messages.usage-setpremium"))
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
                    sender.sendMessage(sender.msg("messages.usage-giveticket"))
                    return true
                }
                val target = requireOnlinePlayer(sender, args[1]) ?: return true
                val amount = args[2].toIntOrNull()
                if (amount == null || amount <= 0) {
                    sender.sendMessage(sender.msg("messages.invalid-amount"))
                    return true
                }
                plugin.ticketManager.giveTicket(target, amount)
                sender.sendMessage(sender.msg("messages.given-ticket", "%amount%" to amount.toString(), "%player%" to target.name))
            }
            "boost" -> {
                if (args.size < 2) {
                    if (plugin.boostManager.isActive()) {
                        sender.sendMessage(
                            sender.msg(
                                "messages.boost-status",
                                "%multiplier%" to plugin.boostManager.getGlobalMultiplier().toString(),
                                "%time%" to plugin.boostManager.formatRemaining(sender as? Player)
                            )
                        )
                    } else {
                        sender.sendMessage(sender.msg("messages.usage-boost"))
                    }
                    return true
                }
                if (args[1].equals("off", ignoreCase = true) || args[1].equals("stop", ignoreCase = true)) {
                    plugin.boostManager.clear()
                    sender.sendMessage(sender.msg("messages.boost-disabled"))
                    return true
                }
                val multiplier = args[1].toDoubleOrNull()
                if (multiplier == null || multiplier <= 1.0) {
                    sender.sendMessage(sender.msg("messages.invalid-amount"))
                    return true
                }
                if (args.size < 3) {
                    sender.sendMessage(sender.msg("messages.usage-boost"))
                    return true
                }
                val durationMs = cc.fraio.frpass.managers.BoostManager.parseDurationMillis(args[2])
                if (durationMs == null) {
                    sender.sendMessage(sender.msg("messages.invalid-duration"))
                    return true
                }
                plugin.boostManager.activate(multiplier, durationMs)
                val msgText = plugin.langManager.getMessage(
                    null,
                    "messages.boost-activated",
                    "%multiplier%" to multiplier.toString(),
                    "%time%" to plugin.boostManager.formatRemaining(null)
                )
                for (online in Bukkit.getOnlinePlayers()) {
                    online.sendMessage(msgText)
                }
                if (sender !is Player) {
                    sender.sendMessage(msgText)
                }
            }
            "editor" -> {
                if (sender !is Player) {
                    sender.sendMessage(sender.msg("messages.only-players"))
                    return true
                }
                when (args.getOrNull(1)?.lowercase()) {
                    "quests" -> plugin.editorManager.questMenu.openList(sender, 1)
                    "tiers" -> plugin.editorManager.tierMenu.openList(sender, 1)
                    else -> plugin.editorManager.openHub(sender)
                }
            }
            "ai" -> {
                if (args.size < 2) {
                    sender.sendMessage(sender.msg("messages.ai.help-header"))
                    sender.sendMessage(sender.msg("messages.ai.help-create"))
                    sender.sendMessage(sender.msg("messages.ai.help-edit"))
                    sender.sendMessage(sender.msg("messages.ai.help-status"))
                    return true
                }
                when (args[1].lowercase()) {
                    "status" -> {
                        plugin.aiManager.showStatus(sender)
                    }
                    "edit" -> {
                        if (args.size < 4) {
                            sender.sendMessage(sender.msg("messages.ai.usage-edit"))
                            return true
                        }
                        val questId = args[2]
                        val instruction = args.drop(3).joinToString(" ")
                        plugin.aiManager.editQuest(sender, questId, instruction)
                    }
                    "create" -> {
                        if (args.size < 3) {
                            sender.sendMessage(sender.msg("messages.ai.usage-create"))
                            return true
                        }
                        if (args[2].endsWith(".yml", ignoreCase = true) && args.size >= 4) {
                            val fileName = args[2]
                            val prompt = args.drop(3).joinToString(" ")
                            plugin.aiManager.createQuest(sender, prompt, fileName)
                        } else {
                            val prompt = args.drop(2).joinToString(" ")
                            plugin.aiManager.createQuest(sender, prompt, null)
                        }
                    }
                    else -> {
                        val prompt = args.drop(1).joinToString(" ")
                        plugin.aiManager.createQuest(sender, prompt, null)
                    }
                }
            }
            else -> {
                sender.sendMessage(sender.msg("messages.admin-usage"))
            }
        }
        
        return true
    }

    private fun requireOnlinePlayer(sender: CommandSender, name: String): Player? {
        val target = Bukkit.getPlayerExact(name) ?: Bukkit.getPlayer(name)
        if (target == null) {
            sender.sendMessage(sender.msg("messages.player-not-found"))
        }
        return target
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (!sender.hasPermission("frpass.admin")) return emptyList()
        
        if (args.size == 1) {
            val subCommands = listOf("addxp", "setxp", "setlevel", "reset", "completequest", "setpremium", "giveticket", "boost", "editor", "ai")
            return subCommands.filter { it.startsWith(args[0], ignoreCase = true) }
        } else if (args.size == 2) {
            val cmd = args[0].lowercase()
            if (cmd == "ai") {
                return listOf("create", "edit", "status").filter { it.startsWith(args[1], ignoreCase = true) }
            }
            if (cmd == "boost") {
                return listOf("1.5", "2.0", "3.0", "off").filter { it.startsWith(args[1], ignoreCase = true) }
            }
            if (cmd == "editor") {
                return listOf("quests", "tiers").filter { it.startsWith(args[1], ignoreCase = true) }
            }
            if (cmd in listOf("addxp", "setxp", "setlevel", "reset", "completequest", "setpremium", "giveticket")) {
                return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
            }
        } else if (args.size == 3) {
            val cmd = args[0].lowercase()
            if (cmd == "boost" && !args[1].equals("off", ignoreCase = true)) {
                return listOf("30m", "1h", "2h", "6h", "1d").filter { it.startsWith(args[2], ignoreCase = true) }
            }
            if (cmd == "ai" && args[1].equals("edit", ignoreCase = true)) {
                return plugin.questManager.quests.keys.filter { it.startsWith(args[2], ignoreCase = true) }
            }
            if (cmd == "completequest") {
                val target = Bukkit.getPlayer(args[1])
                val active = target?.let { plugin.playerDataManager.getPlayer(it.uniqueId)?.activeQuests }
                val pool = if (!active.isNullOrEmpty()) active else plugin.questManager.quests.keys
                return pool.filter { it.startsWith(args[2], ignoreCase = true) }
            }
            if (cmd == "addxp" || cmd == "setxp" || cmd == "giveticket") {
                if (args[2].isEmpty()) return listOf("<amount>")
            } else if (cmd == "setlevel") {
                val maxTier = plugin.tierManager.tiers.keys.maxOrNull() ?: 10
                val suggestions = (0..maxTier).map { it.toString() }
                return suggestions.filter { it.startsWith(args[2], ignoreCase = true) }
            } else if (cmd == "setpremium") {
                return listOf("true", "false").filter { it.startsWith(args[2], ignoreCase = true) }
            }
        }
        
        return emptyList()
    }
}
