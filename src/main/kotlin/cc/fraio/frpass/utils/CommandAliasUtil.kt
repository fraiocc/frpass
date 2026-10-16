package cc.fraio.frpass.utils

import cc.fraio.frpass.FrPass
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.PluginIdentifiableCommand
import org.bukkit.plugin.Plugin
import java.lang.reflect.Field

class DynamicPassCommand(
    name: String,
    private val plugin: FrPass,
    aliases: List<String> = emptyList()
) : Command(name, "Dynamic FrPass command alias", "/$name", aliases), PluginIdentifiableCommand {

    override fun getPlugin(): Plugin = plugin

    override fun execute(sender: org.bukkit.command.CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        val passCommand = plugin.passCommand ?: return false
        return passCommand.onCommand(sender, this, commandLabel, args)
    }

    override fun tabComplete(
        sender: org.bukkit.command.CommandSender,
        alias: String,
        args: Array<out String>
    ): List<String> {
        val passCommand = plugin.passCommand ?: return emptyList()
        return passCommand.onTabComplete(sender, this, alias, args)
    }
}

object CommandAliasUtil {

    private val registeredCommands = mutableListOf<DynamicPassCommand>()

    private fun getCommandMap(): CommandMap? {
        return try {
            val server = Bukkit.getServer()
            val field: Field = server.javaClass.getDeclaredField("commandMap")
            field.isAccessible = true
            field.get(server) as? CommandMap
        } catch (_: Throwable) {
            null
        }
    }

    fun registerAliases(plugin: FrPass) {
        val commandMap = getCommandMap() ?: return
        val configuredAliases = plugin.configManager.config.getStringList("settings.command-aliases")
            .map { it.trim().removePrefix("/").lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()

        for (alias in configuredAliases) {
            // Check if already registered or conflicting with default name
            if (alias == "frpass" || registeredCommands.any { it.name == alias }) continue

            try {
                val dynCmd = DynamicPassCommand(alias, plugin)
                commandMap.register(plugin.name.lowercase(), dynCmd)
                registeredCommands.add(dynCmd)
            } catch (_: Throwable) {}
        }
    }
}
