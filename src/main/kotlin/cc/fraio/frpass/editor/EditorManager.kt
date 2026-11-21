package cc.fraio.frpass.editor

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.menus.MenuSupport
import cc.fraio.frpass.utils.msg
import cc.fraio.frpass.utils.sendMsg
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

class EditorManager(private val plugin: FrPass) {

    companion object {
        private val VALID_ID_REGEX = Regex("^[a-z0-9_-]{1,64}$")
    }

    enum class MenuType {
        HUB,
        QUEST_LIST,
        QUEST_EDIT,
        TIER_LIST,
        TIER_EDIT
    }

    val questStorage = QuestStorage(plugin)
    val tierStorage = TierStorage(plugin)
    val questMenu = QuestEditorMenu(plugin, this)
    val tierMenu = TierEditorMenu(plugin, this)

    private val openEditorMenus = ConcurrentHashMap<UUID, Pair<MenuType, Any>>()
    private val activeSessions = ConcurrentHashMap<UUID, EditorSession>()

    fun isEditorMenu(player: Player): Boolean = openEditorMenus.containsKey(player.uniqueId)
    fun trackOpenMenu(player: Player, type: MenuType, state: Any) {
        openEditorMenus[player.uniqueId] = Pair(type, state)
    }
    fun closeEditorMenu(player: Player) {
        openEditorMenus.remove(player.uniqueId)
    }

    fun hasActiveSession(player: Player): Boolean = activeSessions.containsKey(player.uniqueId)
    fun getActiveSession(player: Player): EditorSession? = activeSessions[player.uniqueId]
    fun endSession(player: Player) {
        activeSessions.remove(player.uniqueId)
    }

    fun refreshOpenEditors() {
        openEditorMenus.toMap().forEach { (uuid, pair) ->
            val player = Bukkit.getPlayer(uuid)
            if (player != null && player.isOnline) {
                plugin.foliaLib.scheduler.runAtEntity(player, Consumer { _ ->
                    if (!player.isOnline) return@Consumer
                    when (pair.first) {
                        MenuType.HUB -> openHub(player)
                        MenuType.QUEST_LIST -> questMenu.openList(player, (pair.second as? Int) ?: 1)
                        MenuType.QUEST_EDIT -> (pair.second as? QuestEditorData)?.let { questMenu.openEdit(player, it) }
                        MenuType.TIER_LIST -> tierMenu.openList(player, (pair.second as? Int) ?: 1)
                        MenuType.TIER_EDIT -> (pair.second as? TierEditorData)?.let { tierMenu.openEdit(player, it) }
                    }
                })
            } else {
                openEditorMenus.remove(uuid)
            }
        }
    }

    fun openHub(player: Player) {
        val config = plugin.menuManager.getMenuConfig("editor_hub_menu.yml")
        val size = config.getInt("menu.size", 27)
        val inv = MenuSupport.createInventory(
            EditorInventoryHolder(MenuType.HUB, 1),
            size,
            player,
            config.getString("menu.title", "&8FrPass Editor")!!
        )
        val itemsSection = config.getConfigurationSection("menu.items")
        if (itemsSection != null) {
            for (key in itemsSection.getKeys(false)) {
                MenuSupport.placeSectionItem(inv, itemsSection, key, player, size)
            }
        }
        player.openInventory(inv)
        trackOpenMenu(player, MenuType.HUB, 1)
    }

    fun promptQuestInput(player: Player, data: QuestEditorData, inputType: EditorInputType, promptText: String) {
        activeSessions[player.uniqueId] = EditorSession(questData = data, inputType = inputType, prompt = promptText)
        player.closeInventory()
        player.sendMessage(player.msg("messages.editor.prompt-input", "%prompt%" to promptText))
    }

    fun promptTierInput(player: Player, data: TierEditorData, inputType: EditorInputType, promptText: String) {
        activeSessions[player.uniqueId] = EditorSession(tierData = data, inputType = inputType, prompt = promptText)
        player.closeInventory()
        player.sendMessage(player.msg("messages.editor.prompt-input", "%prompt%" to promptText))
    }

    fun applySessionInput(player: Player, session: EditorSession, input: String) {
        endSession(player)
        val trimmed = input.trim()

        plugin.foliaLib.scheduler.runAtEntity(player, Consumer { _ ->
            if (!player.isOnline) return@Consumer
            if (trimmed.equals("cancel", ignoreCase = true)) {
                player.sendMessage(player.msg("messages.editor.cancelled"))
                session.questData?.let { questMenu.openEdit(player, it) }
                session.tierData?.let { tierMenu.openEdit(player, it) }
                return@Consumer
            }

            session.questData?.let {
                applyQuestInput(player, it, session.inputType, trimmed)
                questMenu.openEdit(player, it)
                return@Consumer
            }
            session.tierData?.let {
                applyTierInput(player, it, session.inputType, trimmed)
                tierMenu.openEdit(player, it)
            }
        })
    }

    private fun applyQuestInput(player: Player, data: QuestEditorData, type: EditorInputType, trimmed: String) {
        val isClear = trimmed.equals("none", ignoreCase = true) || trimmed.equals("clear", ignoreCase = true)
        when (type) {
            EditorInputType.ID -> {
                val formattedId = trimmed.lowercase().replace(" ", "_")
                if (VALID_ID_REGEX.matches(formattedId)) {
                    data.id = formattedId
                } else {
                    player.sendMsg("messages.editor.invalid-id")
                }
            }
            EditorInputType.DISPLAY_NAME -> data.displayName = trimmed
            EditorInputType.TARGET -> data.target = if (isClear) "" else trimmed.uppercase()
            EditorInputType.REQUIRED_AMOUNT -> parseIntInRange(player, trimmed, 1)?.let { data.requiredAmount = it }
            EditorInputType.REWARD_XP -> parseIntInRange(player, trimmed, 0)?.let { data.rewardXp = it }
            EditorInputType.ADD_EXTRA_REWARD -> data.extraRewards.add(trimmed)
            EditorInputType.ICON_MATERIAL -> data.material = if (isClear) null else trimmed
            EditorInputType.CUSTOM_MODEL_DATA -> parseIntInRange(player, trimmed, 0)?.let { data.customModelData = it }
            else -> {}
        }
    }

    private fun applyTierInput(player: Player, data: TierEditorData, type: EditorInputType, trimmed: String) {
        val isClear = trimmed.equals("none", ignoreCase = true) || trimmed.equals("clear", ignoreCase = true)
        when (type) {
            EditorInputType.TIER_LEVEL -> parseIntInRange(player, trimmed, 1)?.let { data.level = it }
            EditorInputType.TIER_REQUIRED_XP -> parseIntInRange(player, trimmed, 1)?.let { data.requiredXp = it }
            EditorInputType.TIER_DISPLAY_NAME -> data.customName = if (isClear) null else trimmed
            EditorInputType.TIER_ICON_MATERIAL -> data.customIcon = if (isClear) null else trimmed
            EditorInputType.TIER_CUSTOM_MODEL_DATA -> parseIntInRange(player, trimmed, 0)?.let { data.customModelData = it }
            EditorInputType.TIER_ADD_FREE_REWARD -> data.freeRewards.add(trimmed)
            EditorInputType.TIER_ADD_PREMIUM_REWARD -> data.premiumRewards.add(trimmed)
            else -> {}
        }
    }

    private fun parseIntInRange(player: Player, input: String, min: Int): Int? {
        val value = input.toIntOrNull()
        if (value == null || value < min) {
            player.sendMsg("messages.invalid-amount")
            return null
        }
        return value
    }
}

open class EditorInventoryHolder(val type: EditorManager.MenuType, val data: Any) : InventoryHolder {
    override fun getInventory(): Inventory = error("Editor GUI")
}
