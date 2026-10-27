package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader

class ConfigManager(private val plugin: FrPass) {

    lateinit var config: YamlConfiguration
        private set
    lateinit var passMenu: YamlConfiguration
        private set

    fun loadAll() {
        config = loadConfig("config.yml")
        passMenu = loadConfig("menus/pass_menu.yml")
        loadConfig("menus/main_menu.yml")
        loadConfig("menus/quests_menu.yml")
    }

    private fun loadConfig(fileName: String): YamlConfiguration {
        val file = File(plugin.dataFolder, fileName)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            plugin.saveResource(fileName, false)
        }
        val config = YamlConfiguration.loadConfiguration(file)
        
        val defaultStream = plugin.getResource(fileName)
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
        
        return config
    }
    
    fun reload() {
        loadAll()
    }
}
