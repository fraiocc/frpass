package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.api.events.FrPassLevelUpEvent
import cc.fraio.frpass.data.PlayerData
import cc.fraio.frpass.utils.msg
import org.bukkit.Bukkit
import org.bukkit.Sound
import java.sql.Connection
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class PlayerDataManager(private val plugin: FrPass) {
    private val cache = ConcurrentHashMap<UUID, PlayerData>()

    fun init() {
        plugin.foliaLib.impl.runTimerAsync(Consumer { _ ->
            for (player in Bukkit.getOnlinePlayers()) {
                savePlayer(player.uniqueId)
            }
        }, 5L, 5L, TimeUnit.MINUTES)
    }

    fun loadPlayer(uuid: UUID) {
        plugin.foliaLib.impl.runAsync(Consumer { _ ->
            try {
                plugin.databaseManager.getConnection()?.use { conn ->
                    conn.prepareStatement("SELECT * FROM player_data WHERE uuid = ?").use { stmt ->
                        stmt.setString(1, uuid.toString())
                        stmt.executeQuery().use { rs ->
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
                                } catch (_: Exception) {}

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
                                    assignQuests(data)
                                } else if (mode == "ALL") {
                                    data.activeQuests = plugin.questManager.quests.keys.toMutableList()
                                }

                                if (data.activeQuests.isEmpty() || data.activeQuests.none { plugin.questManager.quests.containsKey(it) }) {
                                    assignQuests(data)
                                }

                                cache[uuid] = data
                            } else {
                                val data = PlayerData(uuid)
                                val currentCycle = plugin.cycleManager.getCurrentCycle()
                                data.questCycle = currentCycle
                                assignQuests(data)

                                cache[uuid] = data
                                conn.prepareStatement("INSERT INTO player_data (uuid, xp, level, premium, claimed_tiers, claimed_premium_tiers, quests_progress, quest_cycle, active_quests, pending_tickets) VALUES (?, 0, 1, 0, '', '', '', ?, ?, 0)").use { insert ->
                                    insert.setString(1, uuid.toString())
                                    insert.setInt(2, data.questCycle)
                                    insert.setString(3, data.activeQuests.joinToString(","))
                                    insert.executeUpdate()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load player data for $uuid: ${e.message}")
            }
        })
    }

    private fun assignQuests(data: PlayerData) {
        val mode = plugin.configManager.config.getString("quest-system.mode", "ROTATING")?.uppercase()
        val allQuests = plugin.questManager.quests.keys.toList()
        if (allQuests.isEmpty()) {
            data.activeQuests = mutableListOf()
            return
        }

        val amount = plugin.configManager.config.getInt("quest-system.rotate-amount", 5)
        data.activeQuests = if (mode == "ALL" || allQuests.size <= amount) {
            allQuests.toMutableList()
        } else {
            allQuests.shuffled().take(amount).toMutableList()
        }
    }

    fun savePlayer(uuid: UUID) {
        val data = cache[uuid] ?: return
        saveAsync(data)
    }

    private fun saveAsync(data: PlayerData) {
        plugin.foliaLib.impl.runAsync(Consumer { _ ->
            try {
                plugin.databaseManager.getConnection()?.use { conn ->
                    executeUpdate(conn, data)
                }
            } catch (e: Exception) {
                plugin.logger.severe("Failed to async save player data for ${data.uuid}: ${e.message}")
            }
        })
    }

    fun saveSync(data: PlayerData) {
        try {
            plugin.databaseManager.getConnection()?.use { conn ->
                executeUpdate(conn, data)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to sync save player data for ${data.uuid}: ${e.message}")
        }
    }

    private fun executeUpdate(conn: Connection, data: PlayerData) {
        conn.prepareStatement("UPDATE player_data SET xp = ?, level = ?, premium = ?, claimed_tiers = ?, claimed_premium_tiers = ?, quests_progress = ?, quest_cycle = ?, active_quests = ?, pending_tickets = ? WHERE uuid = ?").use { stmt ->
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
            stmt.setString(10, data.uuid.toString())
            stmt.executeUpdate()
        }
    }

    fun getPlayer(uuid: UUID): PlayerData? {
        return cache[uuid]
    }

    fun unloadPlayer(uuid: UUID) {
        val data = cache.remove(uuid) ?: return
        saveAsync(data)
    }

    fun saveAllSync() {
        for ((_, data) in cache) {
            saveSync(data)
        }
        cache.clear()
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
                val event = FrPassLevelUpEvent(player, oldLevel, data.level)
                Bukkit.getPluginManager().callEvent(event)
                
                if (event.isCancelled) {
                    data.level = oldLevel
                    data.xp += currentTier.requiredXp
                    break
                }
                
                val prefix = plugin.configManager.config.getString("settings.prefix") ?: ""
                player.sendMessage(player.msg("messages.level-up", 
                    "%prefix%" to prefix,
                    "%tier%" to data.level.toString()
                ))
                val soundStr = plugin.configManager.config.getString("settings.sounds.level-up")
                if (soundStr != null && soundStr.isNotEmpty()) {
                    try {
                        player.playSound(player.location, Sound.valueOf(soundStr), 1f, 1f)
                    } catch (_: Exception) {}
                }
            }
            
            currentTier = plugin.tierManager.getTier(data.level)
        }
    }
}
