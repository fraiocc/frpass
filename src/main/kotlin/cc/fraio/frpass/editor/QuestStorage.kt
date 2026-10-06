package cc.fraio.frpass.editor

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.quests.Quest
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class QuestStorage(private val plugin: FrPass) {

    private val questsFolder = File(plugin.dataFolder, "core/quests")

    init {
        if (!questsFolder.exists()) {
            questsFolder.mkdirs()
        }
    }

    fun saveQuest(editorData: QuestEditorData): Boolean {
        return try {
            val quest = editorData.toQuest()
            val targetFile = findFileForQuest(editorData.originalId) ?: File(questsFolder, "custom_quests.yml")

            if (!targetFile.exists()) {
                targetFile.createNewFile()
            }

            val config = YamlConfiguration.loadConfiguration(targetFile)
            
            // If renamed remove old key
            if (editorData.originalId != null && editorData.originalId != quest.id) {
                config.set("quests.${editorData.originalId}", null)
            }

            val path = "quests.${quest.id}"
            config.set("$path.type", quest.type.name)
            if (quest.displayName != null) {
                config.set("$path.display-name", quest.displayName)
            } else {
                config.set("$path.display-name", null)
            }
            if (quest.target != null) {
                config.set("$path.target", quest.target)
            } else {
                config.set("$path.target", null)
            }
            config.set("$path.required-amount", quest.requiredAmount)
            config.set("$path.reward-xp", quest.rewardXp)
            config.set("$path.extra-rewards", quest.extraRewards)

            config.save(targetFile)
            plugin.questManager.loadAll()
            true
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save quest ${editorData.id}: ${e.message}")
            false
        }
    }

    fun deleteQuest(questId: String): Boolean {
        return try {
            val file = findFileForQuest(questId) ?: return false
            val config = YamlConfiguration.loadConfiguration(file)
            config.set("quests.$questId", null)
            config.save(file)
            plugin.questManager.loadAll()
            true
        } catch (e: Exception) {
            plugin.logger.severe("Failed to delete quest $questId: ${e.message}")
            false
        }
    }

    private fun findFileForQuest(questId: String?): File? {
        if (questId == null) return null
        val files = questsFolder.listFiles()?.filter { it.extension == "yml" } ?: return null
        for (file in files) {
            val config = YamlConfiguration.loadConfiguration(file)
            if (config.contains("quests.$questId")) {
                return file
            }
        }
        return null
    }
}
