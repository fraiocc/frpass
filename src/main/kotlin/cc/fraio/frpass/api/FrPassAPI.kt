package cc.fraio.frpass.api

import cc.fraio.frpass.quests.QuestType
import org.bukkit.entity.Player
import java.util.UUID

interface FrPassAPI {
    fun getPlayerLevel(uuid: UUID): Int
    fun getPlayerXp(uuid: UUID): Int
    fun isPremium(uuid: UUID): Boolean
    fun setPremium(uuid: UUID, premium: Boolean)
    fun addXp(uuid: UUID, amount: Int)
    fun addQuestProgress(player: Player, type: QuestType, amount: Int)
}
