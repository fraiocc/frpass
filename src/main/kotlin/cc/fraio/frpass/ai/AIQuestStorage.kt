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

    // Read single quest block for AI prompt context
    fun readQuest(questId: String): Pair<File, String>? {
        val file = findFile(questId) ?: return null
        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("quests.$questId") ?: return null

        val yaml = """
            quests:
              $questId:
                type: ${section.getString("type")}
                display-name: "${section.getString("display-name", questId)}"
                target: ${section.getString("target")}
                required-amount: ${section.getInt("required-amount")}
                reward-xp: ${section.getInt("reward-xp")}
                extra-rewards: ${section.getStringList("extra-rewards")}
        """.trimIndent()

        return Pair(file, yaml)
    }

    // Save or update quest while enforcing directory boundaries
    fun saveQuest(questKey: String, questSection: ConfigurationSection, fileName: String? = null): Pair<File, Boolean> {
        val existingFile = findFile(questKey)
        val targetFile = if (existingFile != null) {
            existingFile
        } else {
            val resolvedName = if (!fileName.isNullOrBlank()) {
                if (fileName.endsWith(".yml", ignoreCase = true)) fileName else "$fileName.yml"
            } else {
                "custom_quests.yml"
            }
            File(folder, resolvedName)
        }

        if (!targetFile.canonicalPath.startsWith(folder.canonicalPath)) {
            throw SecurityException("Security violation: path traversal detected (${targetFile.path})")
        }

        if (!targetFile.exists()) {
            targetFile.createNewFile()
        }

        val config = YamlConfiguration.loadConfiguration(targetFile)
        val isUpdate = config.contains("quests.$questKey")
        val path = "quests.$questKey"

        config.set("$path.type", questSection.getString("type", "KILL_MOB"))
        config.set("$path.display-name", questSection.getString("display-name"))
        config.set("$path.target", questSection.getString("target"))
        config.set("$path.required-amount", questSection.getInt("required-amount", 10))
        config.set("$path.reward-xp", questSection.getInt("reward-xp", 50))
        config.set("$path.extra-rewards", questSection.getStringList("extra-rewards"))

        config.save(targetFile)

        return Pair(targetFile, isUpdate)
    }
}
