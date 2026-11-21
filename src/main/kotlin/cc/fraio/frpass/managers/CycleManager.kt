package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.TimeUtils
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CycleManager(private val plugin: FrPass) {

    private val isWeekly: Boolean
        get() = plugin.configManager.config.getString("quest-system.rotate-interval", "DAILY")?.uppercase() == "WEEKLY"

    fun getCurrentCycle(): Int {
        val now = Instant.now().atZone(ZoneId.systemDefault())
        val epochDay = now.toLocalDate().toEpochDay()
        return if (isWeekly) {
            (epochDay / 7).toInt()
        } else {
            epochDay.toInt()
        }
    }

    fun isRotating(): Boolean {
        return plugin.configManager.config.getString("quest-system.mode", "ROTATING")?.uppercase() == "ROTATING"
    }

    fun getRemainingMillis(): Long {
        val zone = ZoneId.systemDefault()
        val now = Instant.now().atZone(zone)
        val epochDay = now.toLocalDate().toEpochDay()
        val nextDay = if (isWeekly) (epochDay / 7 + 1) * 7 else epochDay + 1
        val next = LocalDate.ofEpochDay(nextDay).atStartOfDay(zone)
        return Duration.between(now, next).toMillis().coerceAtLeast(0L)
    }

    fun formatRemaining(player: Player?): String {
        if (!isRotating()) return plugin.langManager.getMessage(player, "time.none")
        return TimeUtils.formatDuration(plugin, player, getRemainingMillis())
    }
}
