package cc.fraio.frpass.ai

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.quests.QuestRepository
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class AIQuestStorage(private val plugin: FrPass) {

    private val repository = QuestRepository(plugin)

    fun readQuest(questId: String): Pair<File, String>? {
        val file = repository.findQuestFile(questId) ?: return null
        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("quests.$questId") ?: return null

        val exportConfig = YamlConfiguration()
        exportConfig.createSection("quests.$questId", section.getValues(true))
        return Pair(file, exportConfig.saveToString().trim())
    }

    data class QuestResult(val key: String, val file: File, val isUpdate: Boolean)

    fun saveAll(
        quests: Map<String, ConfigurationSection>,
        defaultFileName: String? = null
    ): List<QuestResult> {
        val results = mutableListOf<QuestResult>()
        val fileConfigs = mutableMapOf<File, YamlConfiguration>()

        for ((questKey, questSection) in quests) {
            val targetFile = repository.findQuestFile(questKey) ?: run {
                val resolvedName = if (!defaultFileName.isNullOrBlank()) {
                    if (defaultFileName.endsWith(".yml", ignoreCase = true)) defaultFileName else "$defaultFileName.yml"
                } else {
                    "custom_quests.yml"
                }
                repository.resolveSafeFile(resolvedName)
            }

            val config = fileConfigs.getOrPut(targetFile) {
                if (targetFile.exists()) YamlConfiguration.loadConfiguration(targetFile) else YamlConfiguration()
            }

            val isUpdate = config.contains("quests.$questKey")
            config.createSection("quests.$questKey", questSection.getValues(true))
            results.add(QuestResult(questKey, targetFile, isUpdate))
        }

        for ((file, config) in fileConfigs) {
            repository.saveConfig(file, config)
        }

        return results
    }

    fun saveQuest(questKey: String, questSection: ConfigurationSection, fileName: String? = null): Pair<File, Boolean> {
        val res = saveAll(mapOf(questKey to questSection), fileName).first()
        return Pair(res.file, res.isUpdate)
    }
}
