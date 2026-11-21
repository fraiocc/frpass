package cc.fraio.frpass.api

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.model.LeaderboardEntry
import cc.fraio.frpass.model.Tier
import cc.fraio.frpass.quests.Quest
import cc.fraio.frpass.quests.QuestType
import org.bukkit.entity.Player
import java.util.UUID

internal class FrPassAPIImpl(private val plugin: FrPass) : FrPassAPI {

    override fun isLoaded(uuid: UUID): Boolean = plugin.playerDataManager.getPlayer(uuid) != null

    override fun getPlayerLevel(uuid: UUID): Int = plugin.playerDataManager.getPlayer(uuid)?.level ?: 0

    override fun getPlayerXp(uuid: UUID): Int = plugin.playerDataManager.getPlayer(uuid)?.xp ?: 0

    override fun isPremium(uuid: UUID): Boolean = plugin.playerDataManager.getPlayer(uuid)?.premium ?: false

    override fun setPremium(uuid: UUID, premium: Boolean) {
        val data = plugin.playerDataManager.getPlayer(uuid) ?: return
        data.premium = premium
        plugin.playerDataManager.savePlayer(uuid)
    }

    override fun addXp(uuid: UUID, amount: Int) = plugin.playerDataManager.addXp(uuid, amount)

    override fun setXp(uuid: UUID, xp: Int) = plugin.playerDataManager.setXp(uuid, xp)

    override fun setLevel(uuid: UUID, level: Int) = plugin.playerDataManager.setLevel(uuid, level)

    override fun resetPlayer(uuid: UUID) = plugin.playerDataManager.resetPlayer(uuid)

    override fun giveTicket(player: Player, amount: Int) = plugin.ticketManager.giveTicket(player, amount)

    override fun getUnclaimedRewardCount(uuid: UUID): Int = plugin.playerDataManager.getUnclaimedRewardCount(uuid)

    override fun getRequiredXpForNextLevel(uuid: UUID): Int {
        val data = plugin.playerDataManager.getPlayer(uuid) ?: return 0
        return plugin.tierManager.getTier(data.level + 1)?.requiredXp ?: 0
    }

    override fun getRemainingXpForNextLevel(uuid: UUID): Int {
        val data = plugin.playerDataManager.getPlayer(uuid) ?: return 0
        val nextTier = plugin.tierManager.getTier(data.level + 1) ?: return 0
        return (nextTier.requiredXp - data.xp).coerceAtLeast(0)
    }

    override fun getQuest(questId: String): Quest? = plugin.questManager.quests[questId]

    override fun getAllQuests(): Map<String, Quest> = plugin.questManager.quests.toMap()

    override fun getActiveQuests(uuid: UUID): List<String> =
        plugin.playerDataManager.getPlayer(uuid)?.activeQuests?.toList() ?: emptyList()

    override fun getQuestProgress(uuid: UUID, questId: String): Int =
        plugin.playerDataManager.getPlayer(uuid)?.questProgress?.get(questId) ?: 0

    override fun isQuestCompleted(uuid: UUID, questId: String): Boolean {
        val quest = plugin.questManager.quests[questId] ?: return false
        return getQuestProgress(uuid, questId) >= quest.requiredAmount
    }

    override fun completeQuest(player: Player, questId: String): Boolean =
        plugin.questManager.completeQuest(player, questId)

    override fun addQuestProgress(player: Player, type: QuestType, amount: Int) =
        plugin.questManager.handleProgress(player, type, null, amount)

    override fun addQuestProgress(player: Player, questId: String, amount: Int): Boolean =
        plugin.questManager.addQuestProgress(player, questId, amount)

    override fun getTier(level: Int): Tier? = plugin.tierManager.getTier(level)

    override fun getMaxTier(): Int = plugin.tierManager.tiers.keys.maxOrNull() ?: 0

    override fun getGlobalBoostMultiplier(): Double = plugin.boostManager.getGlobalMultiplier()

    override fun getEffectiveBoostMultiplier(player: Player): Double = plugin.boostManager.getEffectiveMultiplier(player)

    override fun setGlobalBoost(multiplier: Double, durationMillis: Long) =
        plugin.boostManager.activate(multiplier, durationMillis)

    override fun clearGlobalBoost() = plugin.boostManager.clear()

    override fun getLeaderboard(): List<LeaderboardEntry> = plugin.leaderboardManager.entries.toList()

    override fun getLeaderboardEntry(rank: Int): LeaderboardEntry? = plugin.leaderboardManager.get(rank)

    override fun openMainMenu(player: Player) = plugin.menuManager.openMainMenu(player)

    override fun openQuestsMenu(player: Player, page: Int) = plugin.menuManager.openQuestsMenu(player, page)

    override fun openPassMenu(player: Player, page: Int) = plugin.menuManager.openPassMenu(player, page)

    override fun openLeaderboardMenu(player: Player) = plugin.menuManager.openLeaderboardMenu(player)
}
