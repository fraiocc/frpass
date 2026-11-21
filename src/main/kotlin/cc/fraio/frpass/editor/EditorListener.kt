package cc.fraio.frpass.editor

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.msg
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack

class EditorListener(private val plugin: FrPass) : Listener {

    private val editor: EditorManager
        get() = plugin.editorManager

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val topInv = event.view.topInventory
        val holder = topInv.holder as? EditorInventoryHolder ?: return

        event.isCancelled = true
        player.updateInventory()

        val isTop = event.clickedInventory == topInv || (event.rawSlot in 0 until topInv.size)
        if (!isTop) return

        val slot = if (event.rawSlot in 0 until topInv.size) event.rawSlot else event.slot

        when (holder.type) {
            EditorManager.MenuType.HUB -> handleHubClick(player, slot)
            EditorManager.MenuType.QUEST_LIST -> handleQuestListClick(player, slot, (holder.data as? Int) ?: 1, event.click)
            EditorManager.MenuType.QUEST_EDIT -> (holder.data as? QuestEditorData)?.let {
                handleQuestEditClick(player, slot, it, event.click, event.cursor)
            }
            EditorManager.MenuType.TIER_LIST -> handleTierListClick(player, slot, (holder.data as? Int) ?: 1, event.click)
            EditorManager.MenuType.TIER_EDIT -> (holder.data as? TierEditorData)?.let {
                handleTierEditClick(player, slot, it, event.click, event.cursor)
            }
        }
    }

    private fun handleHubClick(player: Player, slot: Int) {
        val config = plugin.menuManager.getMenuConfig("editor_hub_menu.yml")
        when (slot) {
            in config.getIntegerList("menu.items.quests_editor.slots").ifEmpty { listOf(11) } -> editor.questMenu.openList(player, 1)
            in config.getIntegerList("menu.items.tiers_editor.slots").ifEmpty { listOf(15) } -> editor.tierMenu.openList(player, 1)
            in config.getIntegerList("menu.items.close_button.slots").ifEmpty { listOf(22) } -> player.closeInventory()
        }
    }

    private fun handleQuestListClick(player: Player, slot: Int, page: Int, clickType: ClickType) {
        val config = plugin.menuManager.getMenuConfig("editor_list_menu.yml")
        val prevSlots = config.getIntegerList("menu.items.prev_page.slots").ifEmpty { listOf(48) }
        val nextSlots = config.getIntegerList("menu.items.next_page.slots").ifEmpty { listOf(50) }
        val createSlots = config.getIntegerList("menu.items.create_quest.slots").ifEmpty { listOf(49) }
        val closeSlots = config.getIntegerList("menu.items.close_button.slots").ifEmpty { listOf(51) }
        val contentSlots = editor.questMenu.getListContentSlots()

        when (slot) {
            in prevSlots -> if (page > 1) editor.questMenu.openList(player, page - 1)
            in nextSlots -> if (page < editor.questMenu.getListTotalPages()) editor.questMenu.openList(player, page + 1)
            in createSlots -> editor.questMenu.openEdit(player, QuestEditorData(isNew = true))
            in closeSlots -> player.closeInventory()
            else -> {
                val idx = contentSlots.indexOf(slot)
                if (idx == -1) return
                val quests = editor.questMenu.getSortedQuests()
                val quest = quests.getOrNull((page - 1) * contentSlots.size + idx) ?: return
                when (clickType) {
                    ClickType.SHIFT_RIGHT -> {
                        val ok = editor.questStorage.deleteQuest(quest.id)
                        val key = if (ok) "messages.editor.quest-deleted" else "messages.editor.quest-delete-failed"
                        player.sendMessage(player.msg(key, "%quest%" to quest.id))
                        editor.questMenu.openList(player, page)
                    }
                    ClickType.RIGHT -> {
                        val copy = QuestEditorData.fromQuest(quest).copy(originalId = null, id = "${quest.id}_copy", isNew = true)
                        editor.questMenu.openEdit(player, copy)
                    }
                    else -> editor.questMenu.openEdit(player, QuestEditorData.fromQuest(quest))
                }
            }
        }
    }

    private fun handleTierListClick(player: Player, slot: Int, page: Int, clickType: ClickType) {
        val config = plugin.menuManager.getMenuConfig("editor_tier_list_menu.yml")
        val backSlots = config.getIntegerList("menu.items.back_button.slots").ifEmpty { listOf(45) }
        val prevSlots = config.getIntegerList("menu.items.prev_page.slots").ifEmpty { listOf(48) }
        val createSlots = config.getIntegerList("menu.items.create_tier.slots").ifEmpty { listOf(49) }
        val nextSlots = config.getIntegerList("menu.items.next_page.slots").ifEmpty { listOf(50) }
        val closeSlots = config.getIntegerList("menu.items.close_button.slots").ifEmpty { listOf(51) }
        val contentSlots = editor.tierMenu.getListContentSlots()

        when (slot) {
            in backSlots -> editor.openHub(player)
            in prevSlots -> if (page > 1) editor.tierMenu.openList(player, page - 1)
            in nextSlots -> if (page < editor.tierMenu.getListTotalPages()) editor.tierMenu.openList(player, page + 1)
            in createSlots -> {
                val nextLevel = (plugin.tierManager.tiers.keys.maxOrNull() ?: 0) + 1
                editor.tierMenu.openEdit(player, TierEditorData(level = nextLevel, isNew = true))
            }
            in closeSlots -> player.closeInventory()
            else -> {
                val idx = contentSlots.indexOf(slot)
                if (idx == -1) return
                val tier = editor.tierMenu.getSortedTiers().getOrNull((page - 1) * contentSlots.size + idx) ?: return
                if (clickType == ClickType.SHIFT_RIGHT) {
                    val ok = editor.tierStorage.deleteTier(tier.level)
                    val key = if (ok) "messages.editor.tier-deleted" else "messages.editor.tier-delete-failed"
                    player.sendMessage(player.msg(key, "%tier%" to tier.level.toString()))
                    editor.tierMenu.openList(player, page)
                } else {
                    editor.tierMenu.openEdit(player, TierEditorData.fromTier(tier))
                }
            }
        }
    }

    private fun handleQuestEditClick(player: Player, slot: Int, data: QuestEditorData, clickType: ClickType, cursor: ItemStack?) {
        val config = plugin.menuManager.getMenuConfig("editor_edit_menu.yml")
        when (slot) {
            config.getInt("menu.items.id.slot", 11) -> {
                if (data.isNew) editor.promptQuestInput(player, data, EditorInputType.ID, player.msg("messages.editor.prompt-id"))
                else player.sendMessage(player.msg("messages.editor.id-immutable"))
            }
            config.getInt("menu.items.display_name.slot", 12) -> {
                editor.promptQuestInput(player, data, EditorInputType.DISPLAY_NAME, player.msg("messages.editor.prompt-display-name"))
            }
            config.getInt("menu.items.type.slot", 13) -> {
                data.type = editor.questMenu.getAdjacentQuestType(data.type, if (clickType.isRightClick) -1 else 1)
                editor.questMenu.openEdit(player, data)
            }
            config.getInt("menu.items.target.slot", 14) -> {
                if (clickType.isRightClick) {
                    data.target = ""
                    editor.questMenu.openEdit(player, data)
                } else {
                    editor.promptQuestInput(player, data, EditorInputType.TARGET, player.msg("messages.editor.prompt-target"))
                }
            }
            config.getInt("menu.items.required_amount.slot", 15) -> when (clickType) {
                ClickType.RIGHT -> { data.requiredAmount += 10; editor.questMenu.openEdit(player, data) }
                ClickType.SHIFT_RIGHT -> { data.requiredAmount = (data.requiredAmount - 10).coerceAtLeast(1); editor.questMenu.openEdit(player, data) }
                else -> editor.promptQuestInput(player, data, EditorInputType.REQUIRED_AMOUNT, player.msg("messages.editor.prompt-required-amount"))
            }
            config.getInt("menu.items.reward_xp.slot", 21) -> when (clickType) {
                ClickType.RIGHT -> { data.rewardXp += 50; editor.questMenu.openEdit(player, data) }
                ClickType.SHIFT_RIGHT -> { data.rewardXp = (data.rewardXp - 50).coerceAtLeast(0); editor.questMenu.openEdit(player, data) }
                else -> editor.promptQuestInput(player, data, EditorInputType.REWARD_XP, player.msg("messages.editor.prompt-reward-xp"))
            }
            config.getInt("menu.items.icon.slot", 22) -> handleIconClick(
                player, clickType, cursor,
                onCopy = { mat, cmd -> data.material = mat; if (cmd != null) data.customModelData = cmd; editor.questMenu.openEdit(player, data) },
                onReset = { data.material = null; data.customModelData = 0; editor.questMenu.openEdit(player, data) },
                onPromptModel = { editor.promptQuestInput(player, data, EditorInputType.CUSTOM_MODEL_DATA, player.msg("messages.editor.prompt-custom-model-data")) },
                onPromptMat = { editor.promptQuestInput(player, data, EditorInputType.ICON_MATERIAL, player.msg("messages.editor.prompt-icon-material")) }
            )
            config.getInt("menu.items.extra_rewards.slot", 23) -> handleRewardListClick(
                player, data.extraRewards, clickType,
                onAdd = { editor.promptQuestInput(player, data, EditorInputType.ADD_EXTRA_REWARD, player.msg("messages.editor.prompt-extra-reward")) },
                onRefresh = { editor.questMenu.openEdit(player, data) }
            )
            config.getInt("menu.items.back_button.slot", 45) -> editor.questMenu.openList(player, 1)
            config.getInt("menu.items.save_button.slot", 49) -> {
                if (data.id.isBlank()) {
                    player.sendMessage(player.msg("messages.editor.enter-id-first"))
                    return
                }
                if (editor.questStorage.saveQuest(data)) {
                    player.sendMessage(player.msg("messages.editor.quest-saved", "%quest%" to data.id))
                    editor.questMenu.openList(player, 1)
                } else {
                    player.sendMessage(player.msg("messages.editor.quest-save-failed"))
                }
            }
            config.getInt("menu.items.delete_button.slot", 53) -> {
                if (!data.isNew && data.originalId != null) {
                    val ok = editor.questStorage.deleteQuest(data.originalId)
                    val key = if (ok) "messages.editor.quest-deleted" else "messages.editor.quest-delete-failed"
                    player.sendMessage(player.msg(key, "%quest%" to data.originalId))
                    if (ok) editor.questMenu.openList(player, 1)
                }
            }
        }
    }

    private fun handleTierEditClick(player: Player, slot: Int, data: TierEditorData, clickType: ClickType, cursor: ItemStack?) {
        val config = plugin.menuManager.getMenuConfig("editor_tier_edit_menu.yml")
        when (slot) {
            config.getInt("menu.items.level.slot", 11) -> {
                if (data.isNew) editor.promptTierInput(player, data, EditorInputType.TIER_LEVEL, player.msg("messages.editor.prompt-tier-level"))
                else player.sendMessage(player.msg("messages.editor.tier-level-immutable"))
            }
            config.getInt("menu.items.display_name.slot", 12) -> {
                if (clickType.isRightClick) {
                    data.customName = null
                    editor.tierMenu.openEdit(player, data)
                } else {
                    editor.promptTierInput(player, data, EditorInputType.TIER_DISPLAY_NAME, player.msg("messages.editor.prompt-display-name"))
                }
            }
            config.getInt("menu.items.required_xp.slot", 13) -> when (clickType) {
                ClickType.RIGHT -> { data.requiredXp += 100; editor.tierMenu.openEdit(player, data) }
                ClickType.SHIFT_RIGHT -> { data.requiredXp = (data.requiredXp - 100).coerceAtLeast(1); editor.tierMenu.openEdit(player, data) }
                else -> editor.promptTierInput(player, data, EditorInputType.TIER_REQUIRED_XP, player.msg("messages.editor.prompt-required-amount"))
            }
            config.getInt("menu.items.icon.slot", 14) -> handleIconClick(
                player, clickType, cursor,
                onCopy = { mat, cmd -> data.customIcon = mat; if (cmd != null) data.customModelData = cmd; editor.tierMenu.openEdit(player, data) },
                onReset = { data.customIcon = null; data.customModelData = 0; editor.tierMenu.openEdit(player, data) },
                onPromptModel = { editor.promptTierInput(player, data, EditorInputType.TIER_CUSTOM_MODEL_DATA, player.msg("messages.editor.prompt-custom-model-data")) },
                onPromptMat = { editor.promptTierInput(player, data, EditorInputType.TIER_ICON_MATERIAL, player.msg("messages.editor.prompt-icon-material")) }
            )
            config.getInt("menu.items.free_rewards.slot", 21) -> handleRewardListClick(
                player, data.freeRewards, clickType,
                onAdd = { editor.promptTierInput(player, data, EditorInputType.TIER_ADD_FREE_REWARD, player.msg("messages.editor.prompt-extra-reward")) },
                onRefresh = { editor.tierMenu.openEdit(player, data) }
            )
            config.getInt("menu.items.premium_rewards.slot", 23) -> handleRewardListClick(
                player, data.premiumRewards, clickType,
                onAdd = { editor.promptTierInput(player, data, EditorInputType.TIER_ADD_PREMIUM_REWARD, player.msg("messages.editor.prompt-extra-reward")) },
                onRefresh = { editor.tierMenu.openEdit(player, data) }
            )
            config.getInt("menu.items.back_button.slot", 45) -> editor.tierMenu.openList(player, 1)
            config.getInt("menu.items.save_button.slot", 49) -> {
                if (editor.tierStorage.saveTier(data)) {
                    player.sendMessage(player.msg("messages.editor.tier-saved", "%tier%" to data.level.toString()))
                    editor.tierMenu.openList(player, 1)
                } else {
                    player.sendMessage(player.msg("messages.editor.tier-save-failed"))
                }
            }
            config.getInt("menu.items.delete_button.slot", 53) -> {
                if (!data.isNew && data.originalLevel != null) {
                    val ok = editor.tierStorage.deleteTier(data.originalLevel)
                    val key = if (ok) "messages.editor.tier-deleted" else "messages.editor.tier-delete-failed"
                    player.sendMessage(player.msg(key, "%tier%" to data.originalLevel.toString()))
                    if (ok) editor.tierMenu.openList(player, 1)
                }
            }
        }
    }

    private fun handleIconClick(
        player: Player,
        clickType: ClickType,
        cursor: ItemStack?,
        onCopy: (String, Int?) -> Unit,
        onReset: () -> Unit,
        onPromptModel: () -> Unit,
        onPromptMat: () -> Unit
    ) {
        if (cursor != null && !cursor.type.isAir) {
            val meta = cursor.itemMeta
            val cmd = if (meta != null && meta.hasCustomModelData()) meta.customModelData else null
            player.sendMessage(player.msg("messages.editor.icon-copied", "%material%" to cursor.type.name))
            onCopy(cursor.type.name, cmd)
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            player.sendMessage(player.msg("messages.editor.icon-reset"))
            onReset()
        } else if (clickType.isRightClick) {
            onPromptModel()
        } else {
            onPromptMat()
        }
    }

    private fun handleRewardListClick(
        player: Player,
        rewards: MutableList<String>,
        clickType: ClickType,
        onAdd: () -> Unit,
        onRefresh: () -> Unit
    ) {
        when (clickType) {
            ClickType.SHIFT_RIGHT -> {
                rewards.clear()
                player.sendMessage(player.msg("messages.editor.rewards-cleared"))
                onRefresh()
            }
            ClickType.RIGHT -> {
                if (rewards.isNotEmpty()) {
                    rewards.removeAt(rewards.lastIndex)
                    player.sendMessage(player.msg("messages.editor.reward-removed-last"))
                }
                onRefresh()
            }
            else -> onAdd()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.view.topInventory.holder is EditorInventoryHolder || editor.isEditorMenu(player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        if ((event.view.topInventory.holder is EditorInventoryHolder || editor.isEditorMenu(player)) && !editor.hasActiveSession(player)) {
            editor.closeEditorMenu(player)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPaperChat(event: AsyncChatEvent) {
        val player = event.player
        val session = editor.getActiveSession(player) ?: return

        event.isCancelled = true
        val plainText = PlainTextComponentSerializer.plainText().serialize(event.message())
        editor.applySessionInput(player, session, plainText)
    }
}
