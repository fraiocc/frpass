package cc.fraio.frpass.tasks

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.quests.QuestType
import com.tcoded.folialib.wrapper.task.WrappedTask
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class QuestTasks(private val plugin: FrPass) {

    private val lastLocations = ConcurrentHashMap<UUID, Location>()
    private val accumulatedWalk = ConcurrentHashMap<UUID, Double>()
    private var walkTask: WrappedTask? = null
    private var playtimeTask: WrappedTask? = null

    fun startTasks() {
        stopTasks()

        walkTask = plugin.foliaLib.scheduler.runTimer(Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                val data = plugin.playerDataManager.getPlayer(player.uniqueId)
                val hasWalkQuest = data?.activeQuests?.any { questId ->
                    plugin.questManager.quests[questId]?.type == QuestType.WALK
                } == true

                if (!hasWalkQuest) {
                    lastLocations.remove(player.uniqueId)
                    accumulatedWalk.remove(player.uniqueId)
                    continue
                }

                plugin.foliaLib.scheduler.runAtEntity(player, Consumer { _ ->
                    if (!player.isOnline) return@Consumer
                    val current = player.location
                    val last = lastLocations[player.uniqueId]

                    if (last != null && last.world == current.world) {
                        val dist = current.distance(last)
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
                })
            }
        }, 1L, 1L, TimeUnit.SECONDS)

        playtimeTask = plugin.foliaLib.scheduler.runTimer(Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                plugin.foliaLib.scheduler.runAtEntity(player, Consumer { _ ->
                    if (!player.isOnline) return@Consumer
                    plugin.playerDataManager.checkCycle(player.uniqueId)
                    plugin.questManager.handleProgress(player, QuestType.PLAYTIME, null, 1)
                })
            }
        }, 1L, 1L, TimeUnit.MINUTES)
    }

    fun stopTasks() {
        walkTask?.cancel()
        walkTask = null
        playtimeTask?.cancel()
        playtimeTask = null
    }
    
    fun cleanPlayer(uuid: UUID) {
        lastLocations.remove(uuid)
        accumulatedWalk.remove(uuid)
    }
}
