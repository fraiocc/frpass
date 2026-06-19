package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.data.PlayerData
import cc.fraio.frpass.utils.ColorUtils
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class PlayerDataManager(private val plugin: FrPass) {
    private val cache = mutableMapOf<UUID, PlayerData>()

    fun init() {
        plugin.foliaLib.impl.runTimerAsync(Consumer { task ->
            for (player in Bukkit.getOnlinePlayers()) {
                savePlayer(player.uniqueId)
            }
        }, 5L, 5L, TimeUnit.MINUTES) // Auto-save every 5 minutes
    }

    fun loadPlayer(uuid: UUID) {
        plugin.foliaLib.impl.runAsync(Consumer { task ->
            val conn = plugin.databaseManager.getConnection() ?: return@Consumer
            val stmt = conn.prepareStatement("SELECT * FROM player_data WHERE uuid = ?")
            stmt.setString(1, uuid.toString())
            val rs = stmt.executeQuery()
            
            if (rs.next()) {
                val xp = rs.getInt("xp")
                val level = rs.getInt("level")
                val premium = rs.getBoolean("premium")
                val claimedStr = rs.getString("claimed_tiers") ?: ""
                val claimedTiers = if (claimedStr.isEmpty()) mutableListOf() else claimedStr.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
                
                val claimedPremiumStr = rs.getString("claimed_premium_tiers") ?: ""
                val claimedPremiumTiers = if (claimedPremiumStr.isEmpty()) mutableListOf() else claimedPremiumStr.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
                
                var pendingTickets = 0
                try {
                    pendingTickets = rs.getInt("pending_tickets")
                } catch (ignored: Exception) {}
                
                val questsStr = rs.getString("quests_progress") ?: ""
                val questProgress = mutableMapOf<String, Int>()
                if (questsStr.isNotEmpty()) {
                    questsStr.split(",").forEach {
                        val parts = it.split(":")
                        if (parts.size == 2) {
                            questProgress[parts[0]] = parts[1].toIntOrNull() ?: 0
                        }
                    }
                }
                
                val cycle = rs.getInt("quest_cycle")
                val activeStr = rs.getString("active_quests") ?: ""
                val activeQuests = if (activeStr.isEmpty()) mutableListOf() else activeStr.split(",").toMutableList()
                
                val data = PlayerData(uuid, xp, level, premium, claimedTiers, claimedPremiumTiers, questProgress, cycle, activeQuests, pendingTickets)
                
                val currentCycle = plugin.cycleManager.getCurrentCycle()
                val mode = plugin.configManager.config.getString("quest-system.mode", "ROTATING")?.uppercase()
                
                if (mode == "ROTATING" && data.questCycle < currentCycle) {
                    data.questCycle = currentCycle
                    data.questProgress.clear()
                    
                    val amount = plugin.configManager.config.getInt("quest-system.rotate-amount", 5)
                    val allQuests = plugin.questManager.quests.keys.toList()
                    data.activeQuests = if (allQuests.size <= amount) {
                        allQuests.toMutableList()
                    } else {
                        allQuests.shuffled().take(amount).toMutableList()
                    }
                } else if (mode == "ALL") {
                    data.activeQuests = plugin.questManager.quests.keys.toMutableList()
                }
                
                cache[uuid] = data
            } else {
                val data = PlayerData(uuid)
                
                val currentCycle = plugin.cycleManager.getCurrentCycle()
                val mode = plugin.configManager.config.getString("quest-system.mode", "ROTATING")?.uppercase()
                
                if (mode == "ROTATING") {
                    data.questCycle = currentCycle
                    val amount = plugin.configManager.config.getInt("quest-system.rotate-amount", 5)
                    val allQuests = plugin.questManager.quests.keys.toList()
                    data.activeQuests = if (allQuests.size <= amount) {
                        allQuests.toMutableList()
                    } else {
                        allQuests.shuffled().take(amount).toMutableList()
                    }
                } else {
                    data.activeQuests = plugin.questManager.quests.keys.toMutableList()
                }
                
                cache[uuid] = data
                val insert = conn.prepareStatement("INSERT INTO player_data (uuid, xp, level, premium, claimed_tiers, claimed_premium_tiers, quests_progress, quest_cycle, active_quests, pending_tickets) VALUES (?, 0, 1, 0, '', '', '', ?, ?, 0)")
                insert.setString(1, uuid.toString())
                insert.setInt(2, data.questCycle)
                insert.setString(3, data.activeQuests.joinToString(","))
                insert.executeUpdate()
                insert.close()
            }
            rs.close()
            stmt.close()
        })
    }

    fun savePlayer(uuid: UUID) {
        val data = cache[uuid] ?: return
        plugin.foliaLib.impl.runAsync(Consumer { task ->
            val conn = plugin.databaseManager.getConnection() ?: return@Consumer
            val stmt = conn.prepareStatement("UPDATE player_data SET xp = ?, level = ?, premium = ?, claimed_tiers = ?, claimed_premium_tiers = ?, quests_progress = ?, quest_cycle = ?, active_quests = ?, pending_tickets = ? WHERE uuid = ?")
            stmt.setInt(1, data.xp)
            stmt.setInt(2, data.level)
            stmt.setBoolean(3, data.premium)
            stmt.setString(4, data.claimedTiers.joinToString(","))
            stmt.setString(5, data.claimedPremiumTiers.joinToString(","))
            val qpStr = data.questProgress.map { "${it.key}:${it.value}" }.joinToString(",")
            stmt.setString(6, qpStr)
            stmt.setInt(7, data.questCycle)
            stmt.setString(8, data.activeQuests.joinToString(","))
            stmt.setInt(9, data.pendingTickets)
            stmt.setString(10, uuid.toString())
            stmt.executeUpdate()
            stmt.close()
        })
    }

    fun getPlayer(uuid: UUID): PlayerData? {
        return cache[uuid]
    }

    fun unloadPlayer(uuid: UUID) {
        savePlayer(uuid)
        cache.remove(uuid)
    }
    
    fun addXp(uuid: UUID, amount: Int) {
        val data = getPlayer(uuid) ?: return
        data.xp += amount
        
        var currentTier = plugin.tierManager.getTier(data.level)
        while (currentTier != null && data.xp >= currentTier.requiredXp) {
            val oldLevel = data.level
            data.xp -= currentTier.requiredXp
            data.level += 1
            
            val player = Bukkit.getPlayer(uuid)
            if (player != null) {
                val event = cc.fraio.frpass.api.events.FrPassLevelUpEvent(player, oldLevel, data.level)
                Bukkit.getPluginManager().callEvent(event)
                
                if (event.isCancelled) {
                    data.level = oldLevel
                    data.xp += currentTier.requiredXp
                    break
                }
                
                val prefix = plugin.configManager.config.getString("settings.prefix") ?: ""
                player.sendMessage(plugin.langManager.getMessage(player, "messages.level-up", 
                    "%prefix%" to prefix,
                    "%tier%" to data.level.toString()
                ))
                val soundStr = plugin.configManager.config.getString("settings.sounds.level-up")
                if (soundStr != null && soundStr.isNotEmpty()) {
                    try {
                        player.playSound(player.location, org.bukkit.Sound.valueOf(soundStr), 1f, 1f)
                    } catch (e: Exception) {}
                }
            }
            
            currentTier = plugin.tierManager.getTier(data.level)
        }
    }
}
