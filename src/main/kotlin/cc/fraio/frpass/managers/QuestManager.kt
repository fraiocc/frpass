package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.quests.Quest
import cc.fraio.frpass.quests.QuestType
import cc.fraio.frpass.utils.ColorUtils
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
            plugin.saveResource("core/quests/example.yml", false)
        }
        
        folder.listFiles()?.filter { it.extension == "yml" }?.forEach { file ->
            val config = YamlConfiguration.loadConfiguration(file)
            val section = config.getConfigurationSection("quests") ?: return@forEach
            
            for (key in section.getKeys(false)) {
                val typeStr = section.getString("$key.type", "BREAK_BLOCK")!!.uppercase()
                val type = try { QuestType.valueOf(typeStr) } catch(e: Exception) { continue }
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
    
    fun getActiveQuests(type: QuestType): List<Quest> {
        return quests.values.filter { it.type == type }
    }

    fun handleProgress(player: Player, type: QuestType, target: String? = null, amount: Int = 1) {
        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
        val active = getActiveQuests(type).filter { data.activeQuests.contains(it.id) }
        
        for (quest in active) {
            // Check if quest target matches
            if (quest.target != null && quest.target.isNotEmpty()) {
                if (!quest.target.equals(target, ignoreCase = true)) {
                    continue
                }
            }
            
            // Check if already completed
            val currentProgress = data.questProgress[quest.id] ?: 0
            if (currentProgress >= quest.requiredAmount) continue
            
            val newProgress = currentProgress + amount
            data.questProgress[quest.id] = newProgress
            
            val isCompleted = newProgress >= quest.requiredAmount
            val event = cc.fraio.frpass.api.events.FrPassQuestProgressEvent(player, quest, currentProgress, newProgress, isCompleted)
            org.bukkit.Bukkit.getPluginManager().callEvent(event)
            
            // Completed!
            if (isCompleted) {
                plugin.playerDataManager.addXp(player.uniqueId, quest.rewardXp)
                plugin.rewardManager.giveRewards(player, quest.extraRewards)
                val prefix = plugin.configManager.config.getString("settings.prefix") ?: ""
                player.sendMessage(plugin.langManager.getMessage(player, "messages.quest-completed", 
                    "%prefix%" to prefix,
                    "%quest%" to (quest.displayName ?: quest.id),
                    "%xp%" to quest.rewardXp.toString()
                ))
                val soundStr = plugin.configManager.config.getString("settings.sounds.quest-complete")
                if (soundStr != null && soundStr.isNotEmpty()) {
                    try {
                        player.playSound(player.location, org.bukkit.Sound.valueOf(soundStr), 1f, 1f)
                    } catch (e: Exception) {}
                }
            }
        }
    }
}
