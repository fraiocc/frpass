package cc.fraio.frpass.commands

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PassCommand(private val plugin: FrPass) : CommandExecutor {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(ColorUtils.colorize("${plugin.configManager.config.getString("settings.prefix")}&cOnly players can use this command."))
            return true
        }

        if (sender is Player) {
            plugin.menuManager.openMainMenu(sender)
        } else {
            sender.sendMessage(plugin.langManager.getMessage(null, "messages.only-players"))
        }
        return true
    }
}
