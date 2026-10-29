package cc.fraio.frpass.ai

import cc.fraio.frpass.FrPass
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class AIQuestStorage(private val plugin: FrPass) {

    val folder: File by lazy {
        val dir = File(plugin.dataFolder, "core/quests")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    fun findFile(questId: String): File? {
        val files = folder.listFiles()?.filter { it.extension.equals("yml", ignoreCase = true) } ?: return null
        for (file in files) {
            val config = YamlConfiguration.loadConfiguration(file)
            if (config.contains("quests.$questId")) {
                return file
            }
        }
        return null
    }

    fun readQuest(questId: String): Pair<File, String>? {
        val file = findFile(questId) ?: return null
        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("quests.$questId") ?: return null

        val exportConfig = YamlConfiguration()
        exportConfig.createSection("quests.$questId", section.getValues(true))
        val yaml = exportConfig.saveToString().trim()

        return Pair(file, yaml)
    }

    data class QuestResult(val key: String, val file: File, val isUpdate: Boolean)

    fun saveAll(
        quests: Map<String, ConfigurationSection>,
        defaultFileName: String? = null
    ): List<QuestResult> {
        val results = mutableListOf<QuestResult>()
        val fileConfigs = mutableMapOf<File, YamlConfiguration>()
        val baseDirPath = folder.toPath().toAbsolutePath().normalize()

        for ((questKey, questSection) in quests) {
            val existingFile = findFile(questKey)
            val targetFile = if (existingFile != null) {
                existingFile
            } else {
                val resolvedName = if (!defaultFileName.isNullOrBlank()) {
                    if (defaultFileName.endsWith(".yml", ignoreCase = true)) defaultFileName else "$defaultFileName.yml"
                } else {
                    "custom_quests.yml"
                }
                File(folder, resolvedName)
            }

            val targetPath = targetFile.toPath().toAbsolutePath().normalize()
            if (!targetPath.startsWith(baseDirPath)) {
                throw SecurityException("Security violation: path traversal detected (${targetFile.path})")
            }

            val config = fileConfigs.getOrPut(targetFile) {
                if (targetFile.exists()) {
                    YamlConfiguration.loadConfiguration(targetFile)
                } else {
                    YamlConfiguration()
                }
            }

            val isUpdate = config.contains("quests.$questKey")
            config.createSection("quests.$questKey", questSection.getValues(true))
            results.add(QuestResult(questKey, targetFile, isUpdate))
        }

        plugin.frSync.suppress()
        for ((file, config) in fileConfigs) {
            if (!file.exists()) file.createNewFile()
            config.save(file)
        }

        return results
    }

    fun saveQuest(questKey: String, questSection: ConfigurationSection, fileName: String? = null): Pair<File, Boolean> {
        val res = saveAll(mapOf(questKey to questSection), fileName).first()
        return Pair(res.file, res.isUpdate)
    }
}
