package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

class LangManager(private val plugin: FrPass) {
    private lateinit var langConfig: YamlConfiguration

    fun load() {
        val lang = plugin.configManager.config.getString("settings.lang", "en") ?: "en"
        val file = File(plugin.dataFolder, "lang/$lang.yml")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            // Try to save resource if it exists in jar
            try {
                plugin.saveResource("lang/$lang.yml", false)
            } catch (e: Exception) {
                // Ignore, might be custom lang
            }
        }
        
        if (file.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(file)
            
            val defaultStream = plugin.getResource("lang/$lang.yml")
            if (defaultStream != null) {
                val defaultConfig = YamlConfiguration.loadConfiguration(java.io.InputStreamReader(defaultStream, kotlin.text.Charsets.UTF_8))
                langConfig.setDefaults(defaultConfig)
                langConfig.options().copyDefaults(true)
                langConfig.save(file)
            }
        } else {
            plugin.logger.warning("Language file lang/$lang.yml not found!")
            langConfig = YamlConfiguration()
        }
    }

    fun getMessage(player: Player?, path: String, vararg placeholders: Pair<String, String>): String {
        var msg = langConfig.getString(path, "&cMissing lang: $path")!!
        for (p in placeholders) {
            msg = msg.replace(p.first, p.second)
        }
        return ColorUtils.colorize(player, msg)
    }

    fun getMessage(path: String, vararg placeholders: Pair<String, String>): String {
        return getMessage(null, path, *placeholders)
    }
}
