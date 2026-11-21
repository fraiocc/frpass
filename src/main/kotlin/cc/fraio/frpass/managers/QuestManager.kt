package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.api.events.FrPassQuestProgressEvent
import cc.fraio.frpass.model.PlayerData
import cc.fraio.frpass.quests.Quest
import cc.fraio.frpass.quests.QuestType
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.SoundUtils
import cc.fraio.frpass.utils.sendMsg
import kotlin.math.roundToInt
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.function.Consumer

class QuestManager(private val plugin: FrPass) {
    companion object {
        private const val CYCLE_BONUS_KEY = "__cycle_bonus__"
    }

    @Volatile
    var quests: Map<String, Quest> = emptyMap()
        private set

    fun loadAll() {
        val loaded = HashMap<String, Quest>()
        val folder = File(plugin.dataFolder, "core/quests")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        
        val autoGenerate = plugin.configManager.config.getBoolean("settings.generate-default-files", true)
        val files = folder.listFiles()?.filter { it.extension.equals("yml", ignoreCase = true) } ?: emptyList()
        if (files.isEmpty() && autoGenerate) {
            plugin.saveResource("core/quests/example.yml", true)
        }

        folder.listFiles()?.filter { it.extension.equals("yml", ignoreCase = true) }?.forEach { file ->
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
                val required = section.getInt("$key.required-amount", 10).coerceAtLeast(1)
                val xp = section.getInt("$key.reward-xp", 50).coerceAtLeast(0)
                val extraRewards = section.getStringList("$key.extra-rewards")
                val material = section.getString("$key.material")
                val customModelData = section.getInt("$key.custom-model-data", 0)
                
                loaded[key] = Quest(key, displayName, type, target, required, xp, extraRewards, material, customModelData)
            }
        }
        quests = loaded
        plugin.logger.info("Loaded ${loaded.size} quests.")
    }

    fun handleProgress(player: Player, type: QuestType, target: String? = null, amount: Int = 1) {
        if (amount <= 0) return
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            if (plugin.configManager.config.getBoolean("quest-system.ignore-creative", true)) return
        }
        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
        if (data.activeQuests.isEmpty()) return
        
        for (questId in data.activeQuests) {
            val quest = quests[questId] ?: continue
            if (quest.type != type) continue
            if (!quest.target.isNullOrEmpty() && !quest.target.equals(target, ignoreCase = true)) continue
            applyQuestProgress(player, data, quest, amount)
        }
    }

    fun addQuestProgress(player: Player, questId: String, amount: Int): Boolean {
        if (amount <= 0) return false
        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return false
        val quest = quests[questId] ?: return false
        if (!applyQuestProgress(player, data, quest, amount)) return false
        plugin.playerDataManager.savePlayer(player.uniqueId)
        return true
    }

    private fun applyQuestProgress(player: Player, data: PlayerData, quest: Quest, amount: Int): Boolean {
        val currentProgress = data.questProgress[quest.id] ?: 0
        if (currentProgress >= quest.requiredAmount) return false

        val newProgress = (currentProgress + amount).coerceAtMost(quest.requiredAmount)
        val isCompleted = newProgress >= quest.requiredAmount
        val event = FrPassQuestProgressEvent(player, quest, currentProgress, newProgress, isCompleted)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return false

        data.questProgress[quest.id] = newProgress
        if (isCompleted) {
            onQuestComplete(player, quest)
        } else if (plugin.configManager.config.getBoolean("settings.quest-actionbar", true)) {
            val percent = ((newProgress * 100.0) / quest.requiredAmount).roundToInt().coerceIn(0, 100)
            val text = plugin.langManager.getMessage(
                player,
                "messages.quest-progress-actionbar",
                "%quest%" to (quest.displayName ?: quest.id),
                "%progress%" to newProgress.toString(),
                "%required%" to quest.requiredAmount.toString(),
                "%percent%" to percent.toString()
            )
            player.sendActionBar(ColorUtils.toComponent(player, text))
        }
        return true
    }

    fun completeQuest(player: Player, questId: String): Boolean {
        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return false
        val quest = quests[questId] ?: return false
        val currentProgress = data.questProgress[quest.id] ?: 0
        if (currentProgress >= quest.requiredAmount) return false

        val event = FrPassQuestProgressEvent(player, quest, currentProgress, quest.requiredAmount, true)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return false

        data.questProgress[quest.id] = quest.requiredAmount
        onQuestComplete(player, quest)
        plugin.playerDataManager.savePlayer(player.uniqueId)
        return true
    }

    private fun onQuestComplete(player: Player, quest: Quest) {
        val multiplier = plugin.boostManager.getEffectiveMultiplier(player)
        val awardedXp = (quest.rewardXp * multiplier).roundToInt().coerceAtLeast(0)
        plugin.playerDataManager.addXp(player.uniqueId, awardedXp)
        plugin.rewardManager.giveRewards(player, quest.extraRewards)
        
        val prefix = plugin.configManager.config.getString("settings.prefix") ?: ""
        val soundStr = plugin.configManager.config.getString("settings.sounds.quest-complete")

        plugin.foliaLib.scheduler.runAtEntity(player, Consumer { _ ->
            if (!player.isOnline) return@Consumer
            player.sendMsg(
                "messages.quest-completed",
                "%prefix%" to prefix,
                "%quest%" to (quest.displayName ?: quest.id),
                "%xp%" to awardedXp.toString()
            )
            SoundUtils.play(player, soundStr)
        })

        checkAllQuestsBonus(player)
    }

    private fun checkAllQuestsBonus(player: Player) {
        val cfg = plugin.configManager.config
        if (!cfg.getBoolean("quest-system.completion-bonus.enabled", true)) return
        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
        if (data.questProgress[CYCLE_BONUS_KEY] == 1) return

        val activeQuests = data.activeQuests.mapNotNull { quests[it] }
        if (activeQuests.isEmpty()) return
        if (activeQuests.any { (data.questProgress[it.id] ?: 0) < it.requiredAmount }) return

        data.questProgress[CYCLE_BONUS_KEY] = 1
        val bonusXp = cfg.getInt("quest-system.completion-bonus.xp", 250).coerceAtLeast(0)
        val bonusRewards = cfg.getStringList("quest-system.completion-bonus.rewards")
        if (bonusXp > 0) plugin.playerDataManager.addXp(player.uniqueId, bonusXp)
        if (bonusRewards.isNotEmpty()) plugin.rewardManager.giveRewards(player, bonusRewards)

        plugin.foliaLib.scheduler.runAtEntity(player, Consumer { _ ->
            if (player.isOnline) {
                player.sendMsg("messages.all-quests-completed", "%xp%" to bonusXp.toString())
            }
        })
    }
}
