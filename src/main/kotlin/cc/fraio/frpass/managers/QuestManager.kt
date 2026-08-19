package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.api.events.FrPassQuestProgressEvent
import cc.fraio.frpass.quests.Quest
import cc.fraio.frpass.quests.QuestType
import cc.fraio.frpass.utils.msg
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

class QuestManager(private val plugin: FrPass) {
    val quests = mutableMapOf<String, Quest>()

    fun loadAll() {
        quests.clear()
        val folder = File(plugin.dataFolder, "core/quests")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        
        val autoGenerate = plugin.configManager.config.getBoolean("settings.generate-default-files", true)
        val files = folder.listFiles()?.filter { it.extension == "yml" } ?: emptyList()
        if (files.isEmpty() && autoGenerate) {
            plugin.saveResource("core/quests/example.yml", true)
        }

        folder.listFiles()?.filter { it.extension == "yml" }?.forEach { file ->
            val config = YamlConfiguration.loadConfiguration(file)
            val section = config.getConfigurationSection("quests") ?: return@forEach
            
            for (key in section.getKeys(false)) {
                val typeStr = section.getString("$key.type", "BREAK_BLOCK")!!.uppercase()
                val type = try {
                    QuestType.valueOf(typeStr)
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("Invalid quest type '$typeStr' in quest '$key' (${file.name}). Skipping.")
                    continue
                }
                
                val displayName = section.getString("$key.display-name")
                val target = section.getString("$key.target")
                val required = section.getInt("$key.required-amount", 10)
                val xp = section.getInt("$key.reward-xp", 50)
                val extraRewards = section.getStringList("$key.extra-rewards")
                
                quests[key] = Quest(key, displayName, type, target, required, xp, extraRewards)
            }
        }
        plugin.logger.info("Loaded ${quests.size} quests.")
    }

    fun handleProgress(player: Player, type: QuestType, target: String? = null, amount: Int = 1) {
        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
        val activeQuests = quests.values.filter { it.type == type && data.activeQuests.contains(it.id) }
        
        for (quest in activeQuests) {
            if (!quest.target.isNullOrEmpty() && !quest.target.equals(target, ignoreCase = true)) {
                continue
            }
            
            val currentProgress = data.questProgress[quest.id] ?: 0
            if (currentProgress >= quest.requiredAmount) continue
            
            val newProgress = currentProgress + amount
            data.questProgress[quest.id] = newProgress
            
            val isCompleted = newProgress >= quest.requiredAmount
            val event = FrPassQuestProgressEvent(player, quest, currentProgress, newProgress, isCompleted)
            Bukkit.getPluginManager().callEvent(event)
            
            if (isCompleted) {
                onQuestComplete(player, quest)
            }
        }
    }

    private fun onQuestComplete(player: Player, quest: Quest) {
        plugin.playerDataManager.addXp(player.uniqueId, quest.rewardXp)
        plugin.rewardManager.giveRewards(player, quest.extraRewards)
        
        val prefix = plugin.configManager.config.getString("settings.prefix") ?: ""
        player.sendMessage(player.msg("messages.quest-completed", 
            "%prefix%" to prefix,
            "%quest%" to (quest.displayName ?: quest.id),
            "%xp%" to quest.rewardXp.toString()
        ))

        val soundStr = plugin.configManager.config.getString("settings.sounds.quest-complete") ?: return
        if (soundStr.isNotEmpty()) {
            try {
                val sound = Sound.valueOf(soundStr.uppercase())
                player.playSound(player.location, sound, 1f, 1f)
            } catch (ignored: Exception) {}
        }
    }
}
