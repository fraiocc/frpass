package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.api.events.FrPassLevelUpEvent
import cc.fraio.frpass.data.PlayerData
import cc.fraio.frpass.utils.msg
import com.tcoded.folialib.wrapper.task.WrappedTask
import org.bukkit.Bukkit
import org.bukkit.Sound
import java.sql.Connection
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class PlayerDataManager(private val plugin: FrPass) {
    private val cache = ConcurrentHashMap<UUID, PlayerData>()
    private var autoSaveTask: WrappedTask? = null

    fun init() {
        autoSaveTask?.cancel()
        autoSaveTask = plugin.foliaLib.scheduler.runTimerAsync(Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                savePlayer(player.uniqueId)
            }
        }, 5L, 5L, TimeUnit.MINUTES)
    }

    fun shutdown() {
        autoSaveTask?.cancel()
        autoSaveTask = null
        saveAllSync()
    }

    fun syncOnlinePlayers() {
        val mode = plugin.configManager.config.getString("quest-system.mode", "ROTATING")?.uppercase()
        val currentCycle = plugin.cycleManager.getCurrentCycle()

        for ((uuid, data) in cache) {
            var changed = false
            if (mode == "ALL") {
                val allKeys = plugin.questManager.quests.keys.toList()
                if (data.activeQuests != allKeys) {
                    data.activeQuests = CopyOnWriteArrayList(allKeys)
                    changed = true
                }
            } else {
                if (data.questCycle < currentCycle) {
                    data.questCycle = currentCycle
                    data.questProgress.clear()
                    assignQuests(data)
                    changed = true
                } else {
                    val validActive = data.activeQuests.filter { plugin.questManager.quests.containsKey(it) }
                    if (validActive.size != data.activeQuests.size || validActive.isEmpty()) {
                        data.activeQuests = CopyOnWriteArrayList(validActive)
                        if (data.activeQuests.isEmpty()) {
                            assignQuests(data)
                        }
                        changed = true
                    }
                }
            }
            if (changed) {
                savePlayer(uuid)
            }
            checkLevelUp(uuid, data)
        }
    }

    fun checkCycle(uuid: UUID) {
        val data = cache[uuid] ?: return
        val currentCycle = plugin.cycleManager.getCurrentCycle()
        val mode = plugin.configManager.config.getString("quest-system.mode", "ROTATING")?.uppercase()

        if (mode == "ROTATING" && data.questCycle < currentCycle) {
            data.questCycle = currentCycle
            data.questProgress.clear()
            assignQuests(data)
            savePlayer(uuid)
        } else if (mode == "ALL" && (data.activeQuests.size != plugin.questManager.quests.size || !data.activeQuests.containsAll(plugin.questManager.quests.keys))) {
            data.activeQuests = CopyOnWriteArrayList(plugin.questManager.quests.keys)
            savePlayer(uuid)
        }
    }

    fun loadPlayer(uuid: UUID) {
        plugin.foliaLib.scheduler.runAsync(Consumer { _ ->
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
                                val claimedTiers = CopyOnWriteArrayList(
                                    if (claimedStr.isEmpty()) emptyList() else claimedStr.split(",").mapNotNull { it.toIntOrNull() }
                                )

                                val claimedPremiumStr = rs.getString("claimed_premium_tiers") ?: ""
                                val claimedPremiumTiers = CopyOnWriteArrayList(
                                    if (claimedPremiumStr.isEmpty()) emptyList() else claimedPremiumStr.split(",").mapNotNull { it.toIntOrNull() }
                                )

                                var pendingTickets = 0
                                try {
                                    pendingTickets = rs.getInt("pending_tickets")
                                } catch (_: Exception) {}

                                val questsStr = rs.getString("quests_progress") ?: ""
                                val questProgress = ConcurrentHashMap<String, Int>()
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
                                val activeQuests = CopyOnWriteArrayList(
                                    if (activeStr.isEmpty()) emptyList() else activeStr.split(",")
                                )

                                val data = PlayerData(uuid, xp, level, premium, claimedTiers, claimedPremiumTiers, questProgress, cycle, activeQuests, pendingTickets)
                                val currentCycle = plugin.cycleManager.getCurrentCycle()
                                val mode = plugin.configManager.config.getString("quest-system.mode", "ROTATING")?.uppercase()

                                if (mode == "ROTATING" && data.questCycle < currentCycle) {
                                    data.questCycle = currentCycle
                                    data.questProgress.clear()
                                    assignQuests(data)
                                } else if (mode == "ALL") {
                                    data.activeQuests = CopyOnWriteArrayList(plugin.questManager.quests.keys)
                                }

                                if (data.activeQuests.isEmpty() || data.activeQuests.none { plugin.questManager.quests.containsKey(it) }) {
                                    assignQuests(data)
                                }

                                if (Bukkit.getPlayer(uuid) != null) {
                                    cache[uuid] = data
                                }
                            } else {
                                val data = PlayerData(uuid)
                                val currentCycle = plugin.cycleManager.getCurrentCycle()
                                data.questCycle = currentCycle
                                assignQuests(data)

                                conn.prepareStatement("INSERT INTO player_data (uuid, xp, level, premium, claimed_tiers, claimed_premium_tiers, quests_progress, quest_cycle, active_quests, pending_tickets) VALUES (?, 0, 0, ?, '', '', '', ?, ?, 0)").use { insert ->
                                    insert.setString(1, uuid.toString())
                                    insert.setBoolean(2, false)
                                    insert.setInt(3, data.questCycle)
                                    insert.setString(4, data.activeQuests.joinToString(","))
                                    insert.executeUpdate()
                                }

                                if (Bukkit.getPlayer(uuid) != null) {
                                    cache[uuid] = data
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
            data.activeQuests = CopyOnWriteArrayList()
            return
        }

        val amount = plugin.configManager.config.getInt("quest-system.rotate-amount", 5)
        data.activeQuests = if (mode == "ALL" || allQuests.size <= amount) {
            CopyOnWriteArrayList(allQuests)
        } else {
            CopyOnWriteArrayList(allQuests.shuffled().take(amount))
        }
    }

    fun savePlayer(uuid: UUID) {
        val data = cache[uuid] ?: return
        saveAsync(data)
    }

    private fun saveAsync(data: PlayerData) {
        plugin.foliaLib.scheduler.runAsync(Consumer { _ ->
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
            val qpStr = data.questProgress.entries.joinToString(",") { "${it.key}:${it.value}" }
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
        if (amount <= 0) return
        val data = getPlayer(uuid) ?: return
        data.xp += amount
        checkLevelUp(uuid, data)
        savePlayer(uuid)
    }

    fun setXp(uuid: UUID, xp: Int) {
        if (xp < 0) return
        val data = getPlayer(uuid) ?: return
        data.xp = xp
        checkLevelUp(uuid, data)
        savePlayer(uuid)
    }

    fun setLevel(uuid: UUID, level: Int) {
        if (level < 0) return
        val data = getPlayer(uuid) ?: return
        data.level = level
        checkLevelUp(uuid, data)
        savePlayer(uuid)
    }

    fun resetPlayer(uuid: UUID) {
        val data = getPlayer(uuid) ?: return
        data.xp = 0
        data.level = 0
        data.premium = false
        data.claimedTiers.clear()
        data.claimedPremiumTiers.clear()
        data.questProgress.clear()
        data.questCycle = plugin.cycleManager.getCurrentCycle()
        assignQuests(data)
        savePlayer(uuid)
    }

    fun getUnclaimedRewardCount(uuid: UUID): Int {
        val data = getPlayer(uuid) ?: return 0
        var count = 0
        for (tier in plugin.tierManager.tiers.values) {
            if (tier.level <= data.level) {
                if (tier.freeRewards.isNotEmpty() && !data.claimedTiers.contains(tier.level)) {
                    count++
                }
                if (data.premium && tier.premiumRewards.isNotEmpty() && !data.claimedPremiumTiers.contains(tier.level)) {
                    count++
                }
            }
        }
        return count
    }

    private fun checkLevelUp(uuid: UUID, data: PlayerData) {
        var nextTier = plugin.tierManager.getTier(data.level + 1)
        var leveledUp = false
        while (nextTier != null && nextTier.requiredXp > 0 && data.xp >= nextTier.requiredXp) {
            val oldLevel = data.level
            data.xp -= nextTier.requiredXp
            data.level += 1
            leveledUp = true
            
            val player = Bukkit.getPlayer(uuid)
            if (player != null) {
                val event = FrPassLevelUpEvent(player, oldLevel, data.level)
                Bukkit.getPluginManager().callEvent(event)
                
                if (event.isCancelled) {
                    data.level = oldLevel
                    data.xp += nextTier.requiredXp
                    break
                }
                
                val newTierLevel = data.level
                val prefix = plugin.configManager.config.getString("settings.prefix") ?: ""
                val soundStr = plugin.configManager.config.getString("settings.sounds.level-up")
                plugin.foliaLib.scheduler.runAtEntity(player, Consumer { _ ->
                    if (!player.isOnline) return@Consumer
                    player.sendMessage(player.msg("messages.level-up", 
                        "%prefix%" to prefix,
                        "%tier%" to newTierLevel.toString()
                    ))
                    if (!soundStr.isNullOrEmpty()) {
                        try {
                            player.playSound(player.location, Sound.valueOf(soundStr.uppercase()), 1f, 1f)
                        } catch (_: Exception) {}
                    }
                })
            }
            
            nextTier = plugin.tierManager.getTier(data.level + 1)
        }
        if (leveledUp) {
            savePlayer(uuid)
        }
    }
}
