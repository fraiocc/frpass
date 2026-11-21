package cc.fraio.frpass.editor

import cc.fraio.frpass.FrPass
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class TierStorage(private val plugin: FrPass) {

    private val tiersFile: File
        get() = File(plugin.dataFolder, "core/tiers.yml").also {
            it.parentFile?.mkdirs()
        }

    fun saveTier(editorData: TierEditorData): Boolean {
        return try {
            val tier = editorData.toTier()
            val file = tiersFile
            if (!file.exists()) {
                plugin.saveResource("core/tiers.yml", false)
            }
            val config = YamlConfiguration.loadConfiguration(file)

            if (editorData.originalLevel != null && editorData.originalLevel != tier.level) {
                config.set("tiers.${editorData.originalLevel}", null)
            }

            val path = "tiers.${tier.level}"
            config.set("$path.required-xp", tier.requiredXp)
            config.set("$path.free-rewards", tier.freeRewards)
            config.set("$path.premium-rewards", tier.premiumRewards)
            config.set("$path.display.material", tier.customIcon)
            config.set("$path.display.name", tier.customName)
            config.set("$path.display.custom-model-data", tier.customModelData)

            if (config.getConfigurationSection("$path.display")?.getKeys(false)?.isEmpty() == true) {
                config.set("$path.display", null)
            }

            plugin.frSync.suppress()
            config.save(file)
            plugin.tierManager.loadAll()
            true
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save tier ${editorData.level}: ${e.message}")
            false
        }
    }

    fun deleteTier(level: Int): Boolean {
        return try {
            val file = tiersFile
            if (!file.exists()) return false
            val config = YamlConfiguration.loadConfiguration(file)
            config.set("tiers.$level", null)
            plugin.frSync.suppress()
            config.save(file)
            plugin.tierManager.loadAll()
            true
        } catch (e: Exception) {
            plugin.logger.severe("Failed to delete tier $level: ${e.message}")
            false
        }
    }
}
