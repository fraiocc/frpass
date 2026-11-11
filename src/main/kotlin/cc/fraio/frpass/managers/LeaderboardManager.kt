package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import com.tcoded.folialib.wrapper.task.WrappedTask
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.TimeUnit

data class LeaderboardEntry(val uuid: UUID, val name: String, val level: Int, val xp: Int)

class LeaderboardManager(private val plugin: FrPass) {

    @Volatile
    var entries: List<LeaderboardEntry> = emptyList()
        private set
    private var task: WrappedTask? = null

    fun start() {
        stop()
        val config = plugin.configManager.config
        val interval = config.getLong("leaderboard.refresh-seconds", 60L).coerceAtLeast(10L)
        task = plugin.foliaLib.scheduler.runTimerAsync(Runnable { refresh() }, 1L, interval, TimeUnit.SECONDS)
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    fun get(rank: Int): LeaderboardEntry? = entries.getOrNull(rank - 1)

    private fun refresh() {
        val size = plugin.configManager.config.getInt("leaderboard.size", 10).coerceIn(1, 100)
        val unknown = plugin.configManager.config.getString("placeholders.top-empty", "-") ?: "-"
        try {
            plugin.databaseManager.getConnection()?.use { conn ->
                conn.prepareStatement("SELECT uuid, level, xp FROM player_data WHERE level > 0 OR xp > 0 ORDER BY level DESC, xp DESC LIMIT ?").use { stmt ->
                    stmt.setInt(1, size)
                    stmt.executeQuery().use { rs ->
                        val result = mutableListOf<LeaderboardEntry>()
                        while (rs.next()) {
                            val uuid = runCatching { UUID.fromString(rs.getString("uuid")) }.getOrNull() ?: continue
                            val name = Bukkit.getOfflinePlayer(uuid).name ?: unknown
                            result.add(LeaderboardEntry(uuid, name, rs.getInt("level"), rs.getInt("xp")))
                        }
                        entries = result
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to refresh leaderboard: ${e.message}")
        }
    }
}
