package cc.fraio.frpass.data

import java.util.UUID

data class PlayerData(
    val uuid: UUID,
    var xp: Int = 0,
    var level: Int = 1,
    var premium: Boolean = false,
    val claimedTiers: MutableList<Int> = mutableListOf(),
    val claimedPremiumTiers: MutableList<Int> = mutableListOf(),
    val questProgress: MutableMap<String, Int> = mutableMapOf(),
    var questCycle: Int = 0,
    var activeQuests: MutableList<String> = mutableListOf(),
    var pendingTickets: Int = 0
)
