package cc.fraio.frpass.model

data class Tier(
    val level: Int,
    val requiredXp: Int,
    val freeRewards: List<String>,
    val premiumRewards: List<String>,
    val customIcon: String? = null,
    val customName: String? = null,
    val customLore: List<String>? = null,
    val customModelData: Int? = null
)
