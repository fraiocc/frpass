package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigManager(private val plugin: FrPass) {

    lateinit var config: YamlConfiguration
        private set
    lateinit var passMenu: YamlConfiguration
        private set

    fun loadAll() {
        config = loadConfig("config.yml")
        passMenu = loadConfig("menus/pass_menu.yml")
        
        // Menü dosyalarının plugin açıldığında anında klasöre çıkması için:
        loadConfig("menus/main_menu.yml")
        loadConfig("menus/quests_menu.yml")
    }

    private fun loadConfig(fileName: String): YamlConfiguration {
        val file = File(plugin.dataFolder, fileName)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            plugin.saveResource(fileName, false)
        }
        val config = YamlConfiguration.loadConfiguration(file)
        
        val defaultStream = plugin.getResource(fileName)
        if (defaultStream != null) {
            val defaultConfig = YamlConfiguration.loadConfiguration(java.io.InputStreamReader(defaultStream, kotlin.text.Charsets.UTF_8))
            config.setDefaults(defaultConfig)
            config.options().copyDefaults(true)
            config.save(file)
        }
        
        return config
    }
    
    fun reload() {
        loadAll()
    }
}
