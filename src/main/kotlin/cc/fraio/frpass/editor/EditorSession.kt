package cc.fraio.frpass.editor

enum class EditorInputType {
    ID,
    DISPLAY_NAME,
    TARGET,
    REQUIRED_AMOUNT,
    REWARD_XP,
    ADD_EXTRA_REWARD
}

data class EditorSession(
    val data: QuestEditorData,
    val inputType: EditorInputType,
    val prompt: String
)
