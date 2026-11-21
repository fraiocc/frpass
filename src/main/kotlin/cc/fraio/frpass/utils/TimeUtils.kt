package cc.fraio.frpass.utils

import cc.fraio.frpass.FrPass
import org.bukkit.entity.Player
import java.time.Duration

object TimeUtils {

    fun formatDuration(plugin: FrPass, player: Player?, millis: Long): String {
        if (millis <= 0L) return plugin.langManager.getMessage(player, "time.none")
        val total = Duration.ofMillis(millis)
        val days = total.toDays()
        val hours = total.toHoursPart()
        val minutes = total.toMinutesPart()
        return when {
            days > 0 -> plugin.langManager.getMessage(player, "time.days", "%d%" to days.toString(), "%h%" to hours.toString())
            hours > 0 -> plugin.langManager.getMessage(player, "time.hours", "%h%" to hours.toString(), "%m%" to minutes.toString())
            else -> plugin.langManager.getMessage(player, "time.minutes", "%m%" to minutes.coerceAtLeast(1).toString())
        }
    }
}
