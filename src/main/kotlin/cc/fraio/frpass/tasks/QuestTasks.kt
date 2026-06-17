package cc.fraio.frpass.tasks

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.quests.QuestType
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.UUID

class QuestTasks(private val plugin: FrPass) {

    private val lastLocations = mutableMapOf<UUID, Location>()
    private val accumulatedWalk = mutableMapOf<UUID, Double>()

    fun startTasks() {
        // Walk Task (Runs every 1 second)
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                val current = player.location
                val last = lastLocations[player.uniqueId]
                
                // If world is same and they moved
                if (last != null && last.world == current.world) {
                    val dist = current.distance(last)
                    // If distance > 25 it's likely a teleport/enderpearl. Skip it.
                    if (dist > 0.1 && dist <= 25.0) {
                        val acc = (accumulatedWalk[player.uniqueId] ?: 0.0) + dist
                        if (acc >= 1.0) {
                            val blocks = acc.toInt()
                            plugin.questManager.handleProgress(player, QuestType.WALK, null, blocks)
                            accumulatedWalk[player.uniqueId] = acc - blocks
                        } else {
                            accumulatedWalk[player.uniqueId] = acc
                        }
                    }
                }
                lastLocations[player.uniqueId] = current
            }
        }, 20L, 20L)

        // Playtime Task (Runs every 1 minute)
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                plugin.questManager.handleProgress(player, QuestType.PLAYTIME, null, 1)
            }
        }, 1200L, 1200L)
    }
    
    fun cleanPlayer(uuid: UUID) {
        lastLocations.remove(uuid)
        accumulatedWalk.remove(uuid)
    }
}
