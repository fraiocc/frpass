package cc.fraio.frpass.quests

enum class QuestType {
    BREAK_BLOCK, PLACE_BLOCK, KILL_PLAYER, KILL_MOB, FISH, CRAFT, PLAYTIME, WALK,
    CONSUME, BREED_ANIMAL, ENCHANT_ITEM, EXP_EARN, SHEAR_SHEEP
}

data class Quest(
    val id: String,
    val displayName: String?,
    val type: QuestType,
    val target: String?,
    val requiredAmount: Int,
    val rewardXp: Int,
    val extraRewards: List<String>
)
