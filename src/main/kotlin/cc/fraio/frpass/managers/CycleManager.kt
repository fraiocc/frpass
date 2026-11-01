package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import java.time.Instant
import java.time.ZoneId

class CycleManager(private val plugin: FrPass) {
    fun getCurrentCycle(): Int {
        val interval = plugin.configManager.config.getString("quest-system.rotate-interval", "DAILY")?.uppercase()
        val now = Instant.now().atZone(ZoneId.systemDefault())
        val epochDay = now.toLocalDate().toEpochDay()
        return if (interval == "WEEKLY") {
            (epochDay / 7).toInt()
        } else {
            epochDay.toInt()
        }
    }
}
