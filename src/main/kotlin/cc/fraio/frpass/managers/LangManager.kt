package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import net.kyori.adventure.text.Component
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class LangManager(private val plugin: FrPass) {

    private val messages = ConcurrentHashMap<String, String>()
    @Volatile private var cachedPrefix: String = "&aFrPass &8» &7"

    fun load() {
        messages.clear()
        cachedPrefix = plugin.configManager.config.getString("settings.prefix", "&aFrPass &8» &7") ?: "&aFrPass &8» &7"

        val lang = (plugin.configManager.config.getString("settings.lang", "en") ?: "en").lowercase()
        val generateDefaults = plugin.configManager.config.getBoolean("settings.generate-default-files", true)
        if (generateDefaults) {
            for (code in listOf("en", "tr", "de", "fr")) {
                val target = File(plugin.dataFolder, "lang/$code.yml")
                if (!target.exists()) {
                    target.parentFile?.mkdirs()
                    try {
                        plugin.saveResource("lang/$code.yml", false)
                    } catch (_: Exception) {}
                }
            }
        }

        val file = File(plugin.dataFolder, "lang/$lang.yml")

        if (!file.exists()) {
            file.parentFile?.mkdirs()
            try {
                plugin.saveResource("lang/$lang.yml", false)
            } catch (_: Exception) {}
        }

        plugin.getResource("lang/$lang.yml")?.use { defaultStream ->
            val defaultConfig = YamlConfiguration.loadConfiguration(InputStreamReader(defaultStream, Charsets.UTF_8))
            cacheConfig(defaultConfig)
        } ?: plugin.getResource("lang/en.yml")?.use { fallbackStream ->
            val fallbackConfig = YamlConfiguration.loadConfiguration(InputStreamReader(fallbackStream, Charsets.UTF_8))
            cacheConfig(fallbackConfig)
        }

        if (file.exists()) {
            val config = YamlConfiguration.loadConfiguration(file)
            cacheConfig(config)
        } else {
            plugin.logger.warning("[Lang] Language file lang/$lang.yml not found.")
        }
    }

    private fun cacheConfig(config: YamlConfiguration) {
        for (key in config.getKeys(true)) {
            if (config.isString(key)) {
                messages[key] = config.getString(key) ?: ""
            } else if (config.isList(key)) {
                messages[key] = config.getStringList(key).joinToString("\n")
            }
        }
    }

    fun getMessage(player: Player?, path: String, vararg placeholders: Pair<String, String>): String {
        return ColorUtils.colorize(player, resolveRaw(path, *placeholders))
    }

    fun getMessage(path: String, vararg placeholders: Pair<String, String>): String {
        return getMessage(null, path, *placeholders)
    }

    fun getComponent(player: Player?, path: String, vararg placeholders: Pair<String, String>): Component {
        return ColorUtils.toComponent(player, resolveRaw(path, *placeholders))
    }

    fun getComponent(path: String, vararg placeholders: Pair<String, String>): Component {
        return getComponent(null, path, *placeholders)
    }

    private fun resolveRaw(path: String, vararg placeholders: Pair<String, String>): String {
        var raw = messages[path] ?: return "&cMissing lang: $path"
        if (raw.contains("%prefix%")) {
            raw = raw.replace("%prefix%", cachedPrefix)
        }
        for (p in placeholders) {
            raw = raw.replace(p.first, p.second)
        }
        return raw
    }
}
