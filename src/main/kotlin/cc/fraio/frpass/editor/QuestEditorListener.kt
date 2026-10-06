package cc.fraio.frpass.editor

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.quests.Quest
import cc.fraio.frpass.utils.ColorUtils
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.AsyncPlayerChatEvent

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
        when (slot) {
            48 -> {
                if (page > 1) editorManager.openQuestListMenu(player, page - 1)
            }
            50 -> {
                editorManager.openQuestListMenu(player, page + 1)
            }
            49 -> {
                val newData = QuestEditorData(isNew = true)
                editorManager.openQuestEditMenu(player, newData)
            }
            51 -> {
                player.closeInventory()
            }
            else -> {
                val contentSlots = mutableListOf<Int>()
                for (row in 1..4) {
                    for (col in 1..7) {
                        contentSlots.add(row * 9 + col)
                    }
                }
                val indexInGrid = contentSlots.indexOf(slot)
                if (indexInGrid != -1) {
                    val quests = plugin.questManager.quests.values.toList()
                    val totalIndex = (page - 1) * 28 + indexInGrid
                    if (totalIndex in quests.indices) {
                        val quest = quests[totalIndex]
                        if (clickType == ClickType.SHIFT_RIGHT) {
                            // Delete Quest
                            val deleted = editorManager.questStorage.deleteQuest(quest.id)
                            if (deleted) {
                                player.sendMessage(ColorUtils.colorize("&aQuest '${quest.id}' was deleted successfully!"))
                            } else {
                                player.sendMessage(ColorUtils.colorize("&cFailed to delete quest '${quest.id}'."))
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
        when (slot) {
            11 -> {
                if (data.isNew) {
                    editorManager.promptInput(player, data, EditorInputType.ID, "Enter quest ID:")
                } else {
                    player.sendMessage(ColorUtils.colorize("&cExisting quest ID cannot be changed."))
                }
            }
            12 -> {
                editorManager.promptInput(player, data, EditorInputType.DISPLAY_NAME, "Enter quest display name:")
            }
            13 -> {
                data.type = editorManager.getNextQuestType(data.type)
                editorManager.openQuestEditMenu(player, data)
            }
            14 -> {
                editorManager.promptInput(player, data, EditorInputType.TARGET, "Enter target (or 'none' to clear):")
            }
            15 -> {
                editorManager.promptInput(player, data, EditorInputType.REQUIRED_AMOUNT, "Enter required amount:")
            }
            21 -> {
                editorManager.promptInput(player, data, EditorInputType.REWARD_XP, "Enter reward XP:")
            }
            23 -> {
                if (clickType.isRightClick) {
                    data.extraRewards.clear()
                    player.sendMessage(ColorUtils.colorize("&cExtra rewards cleared."))
                    editorManager.openQuestEditMenu(player, data)
                } else {
                    editorManager.promptInput(player, data, EditorInputType.ADD_EXTRA_REWARD, "Enter reward (e.g. [MONEY] 1000):")
                }
            }
            45 -> {
                editorManager.openQuestListMenu(player, 1)
            }
            49 -> {
                if (data.id.isBlank()) {
                    player.sendMessage(ColorUtils.colorize("&cPlease set a Quest ID first."))
                    return
                }
                val saved = editorManager.questStorage.saveQuest(data)
                if (saved) {
                    player.sendMessage(ColorUtils.colorize("&aQuest '${data.id}' saved successfully."))
                    editorManager.openQuestListMenu(player, 1)
                } else {
                    player.sendMessage(ColorUtils.colorize("&cFailed to save quest."))
                }
            }
            53 -> {
                if (!data.isNew && data.originalId != null) {
                    val deleted = editorManager.questStorage.deleteQuest(data.originalId)
                    if (deleted) {
                        player.sendMessage(ColorUtils.colorize("&aQuest '${data.originalId}' deleted."))
                        editorManager.openQuestListMenu(player, 1)
                    } else {
                        player.sendMessage(ColorUtils.colorize("&cFailed to delete quest."))
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryDrag(event: org.bukkit.event.inventory.InventoryDragEvent) {
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

    @EventHandler(priority = EventPriority.LOWEST)
    fun onLegacyChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val session = editorManager.getActiveSession(player) ?: return

        event.isCancelled = true
        val message = event.message
        editorManager.applySessionInput(player, session, message)
    }
}
