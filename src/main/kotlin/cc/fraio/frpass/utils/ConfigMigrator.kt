package cc.fraio.frpass.utils

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.menus.MenuManager
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader

class ConfigMigrator(private val plugin: FrPass) {

    fun migrate() {
        val configFile = File(plugin.dataFolder, "config.yml")
        if (!configFile.exists()) return

        val bundled = loadBundled("config.yml") ?: return
        val current = YamlConfiguration.loadConfiguration(configFile).getInt("config-version", 0)
        if (current >= bundled.getInt("config-version", 0)) return

        for (name in MenuManager.MENU_FILES) {
            merge(File(plugin.dataFolder, "menus/$name"), "menus/$name")
        }
        merge(configFile, "config.yml")
    }

    private fun merge(file: File, resource: String) {
        if (!file.exists()) return
        val bundled = loadBundled(resource) ?: return
        val user = YamlConfiguration.loadConfiguration(file)

        var added = 0
        for (key in bundled.getKeys(true)) {
            if (bundled.isConfigurationSection(key) || user.contains(key)) continue
            user.set(key, bundled.get(key))
            user.setComments(key, bundled.getComments(key))
            user.setInlineComments(key, bundled.getInlineComments(key))
            added++
        }
        if (added == 0) return

        user.save(file)
        plugin.logger.info("Added $added new default setting(s) to ${file.name}.")
    }

    private fun loadBundled(resource: String): YamlConfiguration? {
        return plugin.getResource(resource)?.use {
            YamlConfiguration.loadConfiguration(InputStreamReader(it, Charsets.UTF_8))
        }
    }
}
