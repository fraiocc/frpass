package cc.fraio.frpass.data

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class PlayerData(
    val uuid: UUID,
    @Volatile var xp: Int = 0,
    @Volatile var level: Int = 0,
    @Volatile var premium: Boolean = false,
    val claimedTiers: MutableList<Int> = CopyOnWriteArrayList(),
    val claimedPremiumTiers: MutableList<Int> = CopyOnWriteArrayList(),
    val questProgress: MutableMap<String, Int> = ConcurrentHashMap(),
    @Volatile var questCycle: Int = 0,
    var activeQuests: MutableList<String> = CopyOnWriteArrayList(),
    @Volatile var pendingTickets: Int = 0
)
