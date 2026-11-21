package cc.fraio.frpass.editor

enum class EditorInputType {
    ID,
    DISPLAY_NAME,
    TARGET,
    REQUIRED_AMOUNT,
    REWARD_XP,
    ADD_EXTRA_REWARD,
    ICON_MATERIAL,
    CUSTOM_MODEL_DATA,
    TIER_LEVEL,
    TIER_REQUIRED_XP,
    TIER_DISPLAY_NAME,
    TIER_ICON_MATERIAL,
    TIER_CUSTOM_MODEL_DATA,
    TIER_ADD_FREE_REWARD,
    TIER_ADD_PREMIUM_REWARD
}

data class EditorSession(
    val questData: QuestEditorData? = null,
    val tierData: TierEditorData? = null,
    val inputType: EditorInputType,
    val prompt: String
)
