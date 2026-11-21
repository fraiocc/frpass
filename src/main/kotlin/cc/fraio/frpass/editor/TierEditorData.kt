package cc.fraio.frpass.editor

import cc.fraio.frpass.model.Tier

data class TierEditorData(
    val originalLevel: Int? = null,
    var level: Int = 1,
    var requiredXp: Int = 1000,
    var freeRewards: MutableList<String> = mutableListOf(),
    var premiumRewards: MutableList<String> = mutableListOf(),
    var customIcon: String? = null,
    var customName: String? = null,
    var customModelData: Int = 0,
    var isNew: Boolean = true
) {
    companion object {
        fun fromTier(tier: Tier): TierEditorData {
            return TierEditorData(
                originalLevel = tier.level,
                level = tier.level,
                requiredXp = tier.requiredXp,
                freeRewards = tier.freeRewards.toMutableList(),
                premiumRewards = tier.premiumRewards.toMutableList(),
                customIcon = tier.customIcon,
                customName = tier.customName,
                customModelData = tier.customModelData ?: 0,
                isNew = false
            )
        }
    }

    fun toTier(): Tier {
        return Tier(
            level = level.coerceAtLeast(1),
            requiredXp = requiredXp.coerceAtLeast(1),
            freeRewards = freeRewards,
            premiumRewards = premiumRewards,
            customIcon = customIcon?.takeIf { it.isNotBlank() },
            customName = customName?.takeIf { it.isNotBlank() },
            customLore = null,
            customModelData = customModelData.takeIf { it > 0 }
        )
    }
}
