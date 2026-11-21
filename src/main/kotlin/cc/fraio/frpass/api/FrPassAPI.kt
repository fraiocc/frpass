package cc.fraio.frpass.api

import cc.fraio.frpass.model.LeaderboardEntry
import cc.fraio.frpass.model.Tier
import cc.fraio.frpass.quests.Quest
import cc.fraio.frpass.quests.QuestType
import org.bukkit.entity.Player
import java.util.UUID

/** Public API for interacting with FrPass player data, quests, tiers, boosts, leaderboards, and menus. */
interface FrPassAPI {

    /** Returns true if the player's data is currently loaded in memory. */
    fun isLoaded(uuid: UUID): Boolean

    /** Returns the player's current Battlepass tier/level, or 0 if not loaded. */
    fun getPlayerLevel(uuid: UUID): Int

    /** Returns the player's current XP towards the next tier, or 0 if not loaded. */
    fun getPlayerXp(uuid: UUID): Int

    /** Returns true if the player has Premium Battlepass unlocked. */
    fun isPremium(uuid: UUID): Boolean

    /** Grants or revokes Premium Battlepass status and saves asynchronously. */
    fun setPremium(uuid: UUID, premium: Boolean)

    /** Adds XP to the player and processes any resulting level-ups. */
    fun addXp(uuid: UUID, amount: Int)

    /** Sets the player's XP and processes any resulting level-ups. */
    fun setXp(uuid: UUID, xp: Int)

    /** Sets the player's Battlepass level directly. */
    fun setLevel(uuid: UUID, level: Int)

    /** Resets all Battlepass data (level, XP, premium, claimed tiers, quests) for the player. */
    fun resetPlayer(uuid: UUID)

    /** Gives physical Premium Ticket items to the player (queuing leftovers if inventory is full). */
    fun giveTicket(player: Player, amount: Int)

    /** Returns the number of unlocked but unclaimed free and premium tier rewards. */
    fun getUnclaimedRewardCount(uuid: UUID): Int

    /** Returns the XP required for the player's next tier, or 0 if at max tier. */
    fun getRequiredXpForNextLevel(uuid: UUID): Int

    /** Returns the remaining XP needed for the player's next tier, or 0 if at max tier. */
    fun getRemainingXpForNextLevel(uuid: UUID): Int

    /** Returns the quest definition for [questId], or null if not found. */
    fun getQuest(questId: String): Quest?

    /** Returns an immutable snapshot of all loaded quests keyed by ID. */
    fun getAllQuests(): Map<String, Quest>

    /** Returns the list of active quest IDs assigned to the player. */
    fun getActiveQuests(uuid: UUID): List<String>

    /** Returns the player's progress for [questId], or 0 if none. */
    fun getQuestProgress(uuid: UUID, questId: String): Int

    /** Returns true if the player has completed [questId]. */
    fun isQuestCompleted(uuid: UUID, questId: String): Boolean

    /** Force-completes [questId] for the player; returns false if already completed or invalid. */
    fun completeQuest(player: Player, questId: String): Boolean

    /** Advances all matching active quests of [type] for the player. */
    fun addQuestProgress(player: Player, type: QuestType, amount: Int)

    /** Advances a specific quest by [questId] for the player; returns false if not applied. */
    fun addQuestProgress(player: Player, questId: String, amount: Int): Boolean

    /** Returns the tier definition for [level], or null if none exists. */
    fun getTier(level: Int): Tier?

    /** Returns the highest configured tier level, or 0 if none exist. */
    fun getMaxTier(): Int

    /** Returns the active global XP boost multiplier (1.0 when inactive). */
    fun getGlobalBoostMultiplier(): Double

    /** Returns the effective XP multiplier for [player], including global boost and Premium bonus. */
    fun getEffectiveBoostMultiplier(player: Player): Double

    /** Activates a global XP boost for [durationMillis] milliseconds. */
    fun setGlobalBoost(multiplier: Double, durationMillis: Long)

    /** Clears any active global XP boost immediately. */
    fun clearGlobalBoost()

    /** Returns the current cached leaderboard entries ordered by level and XP descending. */
    fun getLeaderboard(): List<LeaderboardEntry>

    /** Returns the leaderboard entry at 1-based [rank], or null if unavailable. */
    fun getLeaderboardEntry(rank: Int): LeaderboardEntry?

    /** Opens the main Battlepass menu for [player]. */
    fun openMainMenu(player: Player)

    /** Opens the quests menu at [page] for [player]. */
    fun openQuestsMenu(player: Player, page: Int = 1)

    /** Opens the Battlepass tiers menu at [page] for [player]. */
    fun openPassMenu(player: Player, page: Int = 1)

    /** Opens the leaderboard menu for [player]. */
    fun openLeaderboardMenu(player: Player)
}
