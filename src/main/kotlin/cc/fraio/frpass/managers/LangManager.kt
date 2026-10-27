package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
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

        val lang = plugin.configManager.config.getString("settings.lang", "en") ?: "en"
        val file = File(plugin.dataFolder, "lang/$lang.yml")

        if (!file.exists()) {
            file.parentFile?.mkdirs()
            try {
                plugin.saveResource("lang/$lang.yml", false)
            } catch (_: Exception) {}
        }

        if (file.exists()) {
            val config = YamlConfiguration.loadConfiguration(file)

            // Auto-append missing default keys from jar without overwriting user changes
            val defaultStream = plugin.getResource("lang/$lang.yml")
            if (defaultStream != null) {
                val defaultConfig = YamlConfiguration.loadConfiguration(InputStreamReader(defaultStream, Charsets.UTF_8))
                var modified = false
                for (key in defaultConfig.getKeys(true)) {
                    if (!config.contains(key)) {
                        config.set(key, defaultConfig.get(key))
                        modified = true
                    }
                }
                if (modified) {
                    config.save(file)
                }
            }

            // Cache all messages
            for (key in config.getKeys(true)) {
                if (config.isString(key)) {
                    messages[key] = config.getString(key) ?: ""
                } else if (config.isList(key)) {
                    messages[key] = config.getStringList(key).joinToString("\n")
                }
            }
        } else {
            plugin.logger.warning("[Lang] Language file lang/$lang.yml not found.")
        }
    }

    fun getMessage(player: Player?, path: String, vararg placeholders: Pair<String, String>): String {
        var raw = messages[path] ?: return ColorUtils.colorize(player, "&cMissing lang: $path")

        if (raw.contains("%prefix%")) {
            raw = raw.replace("%prefix%", cachedPrefix)
        }

        for (i in placeholders.indices) {
            val p = placeholders[i]
            raw = raw.replace(p.first, p.second)
        }

        return ColorUtils.colorize(player, raw)
    }

    fun getMessage(path: String, vararg placeholders: Pair<String, String>): String {
        return getMessage(null, path, *placeholders)
    }
}
