package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ConfigMigrator
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader

class ConfigManager(private val plugin: FrPass) {

    lateinit var config: YamlConfiguration
        private set

    fun loadAll() {
        ConfigMigrator(plugin).migrate()
        config = loadConfig("config.yml")
    }

    private fun loadConfig(fileName: String): YamlConfiguration {
        val file = File(plugin.dataFolder, fileName)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            plugin.saveResource(fileName, false)
        }
        val loaded = YamlConfiguration.loadConfiguration(file)

        plugin.getResource(fileName)?.use { defaultStream ->
            val defaultConfig = YamlConfiguration.loadConfiguration(InputStreamReader(defaultStream, Charsets.UTF_8))
            loaded.setDefaults(defaultConfig)
        }

        return loaded
    }

    fun reload() {
        loadAll()
    }
}
