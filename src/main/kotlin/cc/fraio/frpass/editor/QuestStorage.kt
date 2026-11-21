package cc.fraio.frpass.editor

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.quests.QuestRepository
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class QuestStorage(private val plugin: FrPass) {

    private val repository = QuestRepository(plugin)

    fun saveQuest(editorData: QuestEditorData): Boolean {
        return try {
            val quest = editorData.toQuest()
            val targetFile = repository.findQuestFile(editorData.originalId) ?: File(repository.folder, "custom_quests.yml")
            val config = if (targetFile.exists()) YamlConfiguration.loadConfiguration(targetFile) else YamlConfiguration()

            if (editorData.originalId != null && editorData.originalId != quest.id) {
                config.set("quests.${editorData.originalId}", null)
            }

            val path = "quests.${quest.id}"
            config.set("$path.type", quest.type.name)
            config.set("$path.display-name", quest.displayName)
            config.set("$path.target", quest.target)
            config.set("$path.required-amount", quest.requiredAmount)
            config.set("$path.reward-xp", quest.rewardXp)
            config.set("$path.extra-rewards", quest.extraRewards)
            config.set("$path.material", if (quest.material.isNullOrBlank()) null else quest.material)
            config.set("$path.custom-model-data", if (quest.customModelData > 0) quest.customModelData else null)

            repository.saveConfig(targetFile, config)
            plugin.questManager.loadAll()
            true
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save quest ${editorData.id}: ${e.message}")
            false
        }
    }

    fun deleteQuest(questId: String): Boolean {
        return try {
            val file = repository.findQuestFile(questId) ?: return false
            val config = YamlConfiguration.loadConfiguration(file)
            config.set("quests.$questId", null)
            repository.saveConfig(file, config)
            plugin.questManager.loadAll()
            true
        } catch (e: Exception) {
            plugin.logger.severe("Failed to delete quest $questId: ${e.message}")
            false
        }
    }
}
