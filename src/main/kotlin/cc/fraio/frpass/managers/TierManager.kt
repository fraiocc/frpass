package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

data class Tier(
    val level: Int, 
    val requiredXp: Int, 
    val freeRewards: List<String>, 
    val premiumRewards: List<String>,
    val customIcon: String? = null,
    val customName: String? = null,
    val customLore: List<String>? = null,
    val customModelData: Int? = null
)

class TierManager(private val plugin: FrPass) {
    val tiers = mutableMapOf<Int, Tier>()

    fun loadAll() {
        tiers.clear()
        val file = File(plugin.dataFolder, "core/tiers.yml")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            plugin.saveResource("core/tiers.yml", false)
        }
        val config = YamlConfiguration.loadConfiguration(file)
        
        val section = config.getConfigurationSection("tiers") ?: return
        for (key in section.getKeys(false)) {
            val level = key.toIntOrNull() ?: continue
            val requiredXp = section.getInt("$key.required-xp", 1000)
            val free = section.getStringList("$key.free-rewards")
            val premium = section.getStringList("$key.premium-rewards")
            
            val customIcon = section.getString("$key.display.material")
            val customName = section.getString("$key.display.name")
            val customLore = if (section.contains("$key.display.lore")) section.getStringList("$key.display.lore") else null
            val customModelData = if (section.contains("$key.display.custom-model-data")) section.getInt("$key.display.custom-model-data") else null
            
            tiers[level] = Tier(level, requiredXp, free, premium, customIcon, customName, customLore, customModelData)
        }
        plugin.logger.info("Loaded ${tiers.size} battlepass tiers.")
    }
    
    fun getTier(level: Int): Tier? {
        return tiers[level]
    }
}
