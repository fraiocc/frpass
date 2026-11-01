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
    fun setXp(uuid: UUID, xp: Int)
    fun setLevel(uuid: UUID, level: Int)
    fun resetPlayer(uuid: UUID)
    fun giveTicket(player: Player, amount: Int)
    fun completeQuest(player: Player, questId: String): Boolean
    fun getUnclaimedRewardCount(uuid: UUID): Int
    fun getRequiredXpForNextLevel(uuid: UUID): Int
    fun getRemainingXpForNextLevel(uuid: UUID): Int
    fun getActiveQuests(uuid: UUID): List<String>
    fun getQuestProgress(uuid: UUID, questId: String): Int
    fun addQuestProgress(player: Player, type: QuestType, amount: Int)
    fun addQuestProgress(player: Player, questId: String, amount: Int): Boolean
}
