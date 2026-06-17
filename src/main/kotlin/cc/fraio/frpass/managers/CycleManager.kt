package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import java.time.Instant
import java.time.ZoneId

class CycleManager(private val plugin: FrPass) {
    fun getCurrentCycle(): Int {
        val interval = plugin.configManager.config.getString("quest-system.rotate-interval", "DAILY")?.uppercase()
        val now = Instant.now().atZone(ZoneId.systemDefault())
        return if (interval == "WEEKLY") {
            // Days since epoch / 7
            (now.toEpochSecond() / (86400 * 7)).toInt()
        } else {
            // Days since epoch
            (now.toEpochSecond() / 86400).toInt()
        }
    }
}
