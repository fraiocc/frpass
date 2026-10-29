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

class QuestEditorListener(private val plugin: FrPass) : Listener {

    private val editorManager: QuestEditorManager
        get() = plugin.questEditorManager

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
            QuestEditorManager.EditorMenuType.QUEST_LIST -> {
                val page = (holder.data as? Int) ?: 1
                handleListClick(player, slot, page, event.click)
            }
            QuestEditorManager.EditorMenuType.QUEST_EDIT -> {
                val data = (holder.data as? QuestEditorData) ?: return
                handleEditClick(player, slot, data, event.click)
            }
        }
    }

    private fun handleListClick(player: Player, slot: Int, page: Int, clickType: ClickType) {
        val config = plugin.menuManager.getMenuConfig("editor_list_menu.yml")
        val prevSlots = config.getIntegerList("menu.items.prev_page.slots").ifEmpty { listOf(48) }
        val nextSlots = config.getIntegerList("menu.items.next_page.slots").ifEmpty { listOf(50) }
        val createSlots = config.getIntegerList("menu.items.create_quest.slots").ifEmpty { listOf(49) }
        val closeSlots = config.getIntegerList("menu.items.close_button.slots").ifEmpty { listOf(51) }
        val contentSlots = editorManager.getListContentSlots()
        val totalPages = editorManager.getListTotalPages()

        when (slot) {
            in prevSlots -> {
                if (page > 1) editorManager.openQuestListMenu(player, page - 1)
            }
            in nextSlots -> {
                if (page < totalPages) editorManager.openQuestListMenu(player, page + 1)
            }
            in createSlots -> {
                val newData = QuestEditorData(isNew = true)
                editorManager.openQuestEditMenu(player, newData)
            }
            in closeSlots -> {
                player.closeInventory()
            }
            else -> {
                val indexInGrid = contentSlots.indexOf(slot)
                if (indexInGrid != -1) {
                    val quests = plugin.questManager.quests.values.toList()
                    val totalIndex = (page - 1) * contentSlots.size + indexInGrid
                    if (totalIndex in quests.indices) {
                        val quest = quests[totalIndex]
                        if (clickType == ClickType.SHIFT_RIGHT) {
                            val deleted = editorManager.questStorage.deleteQuest(quest.id)
                            if (deleted) {
                                player.sendMessage(player.msg("messages.editor.quest-deleted", "%quest%" to quest.id))
                            } else {
                                player.sendMessage(player.msg("messages.editor.quest-delete-failed", "%quest%" to quest.id))
                            }
                            editorManager.openQuestListMenu(player, page)
                        } else {
                            val data = QuestEditorData.fromQuest(quest)
                            editorManager.openQuestEditMenu(player, data)
                        }
                    }
                }
            }
        }
    }

    private fun handleEditClick(player: Player, slot: Int, data: QuestEditorData, clickType: ClickType) {
        val config = plugin.menuManager.getMenuConfig("editor_edit_menu.yml")
        val idSlot = config.getInt("menu.items.id.slot", 11)
        val nameSlot = config.getInt("menu.items.display_name.slot", 12)
        val typeSlot = config.getInt("menu.items.type.slot", 13)
        val targetSlot = config.getInt("menu.items.target.slot", 14)
        val amountSlot = config.getInt("menu.items.required_amount.slot", 15)
        val xpSlot = config.getInt("menu.items.reward_xp.slot", 21)
        val rewardsSlot = config.getInt("menu.items.extra_rewards.slot", 23)
        val backSlot = config.getInt("menu.items.back_button.slot", 45)
        val saveSlot = config.getInt("menu.items.save_button.slot", 49)
        val delSlot = config.getInt("menu.items.delete_button.slot", 53)

        when (slot) {
            idSlot -> {
                if (data.isNew) {
                    editorManager.promptInput(player, data, EditorInputType.ID, player.msg("messages.editor.prompt-id"))
                } else {
                    player.sendMessage(player.msg("messages.editor.id-immutable"))
                }
            }
            nameSlot -> {
                editorManager.promptInput(player, data, EditorInputType.DISPLAY_NAME, player.msg("messages.editor.prompt-display-name"))
            }
            typeSlot -> {
                data.type = editorManager.getNextQuestType(data.type)
                editorManager.openQuestEditMenu(player, data)
            }
            targetSlot -> {
                editorManager.promptInput(player, data, EditorInputType.TARGET, player.msg("messages.editor.prompt-target"))
            }
            amountSlot -> {
                editorManager.promptInput(player, data, EditorInputType.REQUIRED_AMOUNT, player.msg("messages.editor.prompt-required-amount"))
            }
            xpSlot -> {
                editorManager.promptInput(player, data, EditorInputType.REWARD_XP, player.msg("messages.editor.prompt-reward-xp"))
            }
            rewardsSlot -> {
                if (clickType.isRightClick) {
                    data.extraRewards.clear()
                    player.sendMessage(player.msg("messages.editor.rewards-cleared"))
                    editorManager.openQuestEditMenu(player, data)
                } else {
                    editorManager.promptInput(player, data, EditorInputType.ADD_EXTRA_REWARD, player.msg("messages.editor.prompt-extra-reward"))
                }
            }
            backSlot -> {
                editorManager.openQuestListMenu(player, 1)
            }
            saveSlot -> {
                if (data.id.isBlank()) {
                    player.sendMessage(player.msg("messages.editor.enter-id-first"))
                    return
                }
                val saved = editorManager.questStorage.saveQuest(data)
                if (saved) {
                    player.sendMessage(player.msg("messages.editor.quest-saved", "%quest%" to data.id))
                    editorManager.openQuestListMenu(player, 1)
                } else {
                    player.sendMessage(player.msg("messages.editor.quest-save-failed"))
                }
            }
            delSlot -> {
                if (!data.isNew && data.originalId != null) {
                    val deleted = editorManager.questStorage.deleteQuest(data.originalId)
                    if (deleted) {
                        player.sendMessage(player.msg("messages.editor.quest-deleted", "%quest%" to data.originalId))
                        editorManager.openQuestListMenu(player, 1)
                    } else {
                        player.sendMessage(player.msg("messages.editor.quest-delete-failed", "%quest%" to data.originalId))
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val topInv = event.view.topInventory
        val player = event.whoClicked as? Player ?: return
        if (topInv.holder is EditorInventoryHolder || editorManager.isEditorMenu(player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val topInv = event.view.topInventory
        if ((topInv.holder is EditorInventoryHolder || editorManager.isEditorMenu(player)) && !editorManager.hasActiveSession(player)) {
            editorManager.closeEditorMenu(player)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPaperChat(event: AsyncChatEvent) {
        val player = event.player
        val session = editorManager.getActiveSession(player) ?: return

        event.isCancelled = true
        val plainText = PlainTextComponentSerializer.plainText().serialize(event.message())
        editorManager.applySessionInput(player, session, plainText)
    }
}
