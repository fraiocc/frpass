package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.TimeUtils
import org.bukkit.entity.Player

class BoostManager(private val plugin: FrPass) {

    @Volatile
    private var globalMultiplier: Double = 1.0

    @Volatile
    private var expiresAtMillis: Long = 0L

    fun activate(multiplier: Double, durationMillis: Long) {
        globalMultiplier = multiplier.coerceAtLeast(1.0)
        expiresAtMillis = System.currentTimeMillis() + durationMillis
    }

    fun clear() {
        globalMultiplier = 1.0
        expiresAtMillis = 0L
    }

    fun isActive(): Boolean {
        if (globalMultiplier <= 1.0 || System.currentTimeMillis() >= expiresAtMillis) {
            if (expiresAtMillis > 0L) clear()
            return false
        }
        return true
    }

    fun getGlobalMultiplier(): Double {
        return if (isActive()) globalMultiplier else 1.0
    }

    fun getEffectiveMultiplier(player: Player?): Double {
        var total = getGlobalMultiplier()
        val data = player?.let { plugin.playerDataManager.getPlayer(it.uniqueId) }
        if (data?.premium == true) {
            val premiumMult = plugin.configManager.config.getDouble("quest-system.premium-xp-multiplier", 1.0)
            if (premiumMult > 1.0) {
                total *= premiumMult
            }
        }
        return total
    }

    fun getRemainingMillis(): Long {
        if (!isActive()) return 0L
        return (expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun formatRemaining(player: Player?): String {
        if (!isActive()) return plugin.langManager.getMessage(player, "time.none")
        return TimeUtils.formatDuration(plugin, player, getRemainingMillis())
    }

    companion object {
        fun parseDurationMillis(raw: String): Long? {
            val trimmed = raw.trim().lowercase()
            if (trimmed.length < 2) return null
            val unit = trimmed.last()
            val value = trimmed.dropLast(1).toLongOrNull() ?: return null
            if (value <= 0) return null
            return when (unit) {
                's' -> value * 1000L
                'm' -> value * 60_000L
                'h' -> value * 3_600_000L
                'd' -> value * 86_400_000L
                else -> null
            }
        }
    }
}
