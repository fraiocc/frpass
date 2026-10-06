package cc.fraio.frpass.editor

import cc.fraio.frpass.quests.Quest
import cc.fraio.frpass.quests.QuestType

data class QuestEditorData(
    val originalId: String? = null,
    var id: String = "",
    var displayName: String = "",
    var type: QuestType = QuestType.BREAK_BLOCK,
    var target: String = "",
    var requiredAmount: Int = 10,
    var rewardXp: Int = 50,
    var extraRewards: MutableList<String> = mutableListOf(),
    var isNew: Boolean = true
) {
    companion object {
        fun fromQuest(quest: Quest): QuestEditorData {
            return QuestEditorData(
                originalId = quest.id,
                id = quest.id,
                displayName = quest.displayName ?: quest.id,
                type = quest.type,
                target = quest.target ?: "",
                requiredAmount = quest.requiredAmount,
                rewardXp = quest.rewardXp,
                extraRewards = quest.extraRewards.toMutableList(),
                isNew = false
            )
        }
    }

    fun toQuest(): Quest {
        return Quest(
            id = id.ifBlank { "quest_${System.currentTimeMillis()}" },
            displayName = displayName.ifBlank { null },
            type = type,
            target = target.ifBlank { null },
            requiredAmount = requiredAmount.coerceAtLeast(1),
            rewardXp = rewardXp.coerceAtLeast(0),
            extraRewards = extraRewards
        )
    }
}
