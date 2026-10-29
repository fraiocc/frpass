package cc.fraio.frpass.utils

import cc.fraio.frpass.FrPass
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.PluginIdentifiableCommand
import org.bukkit.command.SimpleCommandMap
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

    private val reservedNames = setOf("frpass", "pass", "battlepass", "frpassadmin")
    private val registeredCommands = mutableMapOf<String, DynamicPassCommand>()

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

    @Suppress("UNCHECKED_CAST")
    private fun getKnownCommands(commandMap: CommandMap): MutableMap<String, Command>? {
        return try {
            var clazz: Class<*>? = commandMap.javaClass
            while (clazz != null) {
                try {
                    val field = clazz.getDeclaredField("knownCommands")
                    field.isAccessible = true
                    return field.get(commandMap) as? MutableMap<String, Command>
                } catch (_: NoSuchFieldException) {
                    clazz = clazz.superclass
                }
            }
            if (commandMap is SimpleCommandMap) {
                val field = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
                field.isAccessible = true
                return field.get(commandMap) as? MutableMap<String, Command>
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    fun registerAliases(plugin: FrPass) {
        val commandMap = getCommandMap() ?: return
        val knownCommands = getKnownCommands(commandMap)
        val fallbackPrefix = plugin.name.lowercase()

        val configuredAliases = plugin.configManager.config.getStringList("settings.command-aliases")
            .map { it.trim().removePrefix("/").lowercase() }
            .filter { it.isNotEmpty() && it !in reservedNames }
            .distinct()
            .toSet()

        val toRemove = registeredCommands.keys.filter { it !in configuredAliases }
        for (oldAlias in toRemove) {
            val cmd = registeredCommands.remove(oldAlias) ?: continue
            cmd.unregister(commandMap)
            knownCommands?.remove(oldAlias, cmd)
            knownCommands?.remove("$fallbackPrefix:$oldAlias", cmd)
        }

        for (alias in configuredAliases) {
            if (registeredCommands.containsKey(alias)) continue

            try {
                val dynCmd = DynamicPassCommand(alias, plugin)
                commandMap.register(fallbackPrefix, dynCmd)
                registeredCommands[alias] = dynCmd
            } catch (_: Throwable) {}
        }
    }
}
