package cc.fraio.frpass.model

import java.util.UUID

data class LeaderboardEntry(
    val uuid: UUID,
    val name: String,
    val level: Int,
    val xp: Int
)
