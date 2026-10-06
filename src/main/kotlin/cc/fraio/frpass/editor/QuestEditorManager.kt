package cc.fraio.frpass.editor

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.quests.Quest
import cc.fraio.frpass.quests.QuestType
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class QuestEditorManager(private val plugin: FrPass) {

    val questStorage = QuestStorage(plugin)

    enum class EditorMenuType {
        QUEST_LIST,
        QUEST_EDIT
    }

    private val openEditorMenus = ConcurrentHashMap<UUID, Pair<EditorMenuType, Any>>()
    private val activeSessions = ConcurrentHashMap<UUID, EditorSession>()

    fun isEditorMenu(player: Player): Boolean = openEditorMenus.containsKey(player.uniqueId)
    fun getEditorMenuState(player: Player): Pair<EditorMenuType, Any>? = openEditorMenus[player.uniqueId]
    fun closeEditorMenu(player: Player) {
        openEditorMenus.remove(player.uniqueId)
    }

    fun hasActiveSession(player: Player): Boolean = activeSessions.containsKey(player.uniqueId)
    fun getActiveSession(player: Player): EditorSession? = activeSessions[player.uniqueId]
    fun startSession(player: Player, session: EditorSession) {
        activeSessions[player.uniqueId] = session
    }
    fun endSession(player: Player) {
        activeSessions.remove(player.uniqueId)
    }

    fun openQuestListMenu(player: Player, page: Int = 1) {
        val config = plugin.menuManager.getMenuConfig("editor_list_menu.yml")
        val quests = plugin.questManager.quests.values.toList()
        val questSlots = config.getIntegerList("quest-slots").ifEmpty {
            listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43)
        }
        val pageSize = questSlots.size
        val totalPages = ((quests.size - 1) / pageSize + 1).coerceAtLeast(1)
        val currentPage = page.coerceIn(1, totalPages)

        val titleRaw = config.getString("menu.title", "&8Quest Editor (Page %page%/%total%)")!!
            .replace("%page%", currentPage.toString())
            .replace("%total%", totalPages.toString())
        val size = config.getInt("menu.size", 54)

        val inv = Bukkit.createInventory(
            EditorInventoryHolder(EditorMenuType.QUEST_LIST, currentPage),
            size,
            ColorUtils.colorize(player, titleRaw)
        )

        val itemsSection = config.getConfigurationSection("menu.items")
        if (itemsSection != null) {
            for (key in itemsSection.getKeys(false)) {
                val keyName = key.lowercase()
                if (keyName == "prev_page" && currentPage <= 1) continue
                if (keyName == "next_page" && currentPage >= totalPages) continue

                val matStr = itemsSection.getString("$key.material", "STONE")!!
                val name = itemsSection.getString("$key.name", "")!!
                val lore = itemsSection.getStringList("$key.lore")
                val modelData = itemsSection.getInt("$key.custom-model-data", 0)
                val slots = itemsSection.getIntegerList("$key.slots")

                val item = ItemBuilder.fromIdentifier(matStr)
                    .setName(name, player)
                    .setLore(lore, player)
                    .setCustomModelData(modelData)
                    .build()

                for (s in slots) {
                    if (s in 0 until size) inv.setItem(s, item)
                }
            }
        }

        val startIndex = (currentPage - 1) * pageSize
        val pageQuests = quests.drop(startIndex).take(pageSize)

        for (i in pageQuests.indices) {
            val quest = pageQuests[i]
            val slot = questSlots[i]
            val questItem = createQuestDisplayItem(quest, player)
            inv.setItem(slot, questItem)
        }

        openEditorMenus[player.uniqueId] = Pair(EditorMenuType.QUEST_LIST, currentPage)
        player.openInventory(inv)
    }

    private fun createQuestDisplayItem(quest: Quest, player: Player): org.bukkit.inventory.ItemStack {
        val mat = when (quest.type) {
            QuestType.BREAK_BLOCK -> Material.DIAMOND_PICKAXE
            QuestType.PLACE_BLOCK -> Material.BRICKS
            QuestType.KILL_MOB -> Material.IRON_SWORD
            QuestType.KILL_PLAYER -> Material.DIAMOND_SWORD
            QuestType.FISH -> Material.FISHING_ROD
            QuestType.CRAFT -> Material.CRAFTING_TABLE
            QuestType.PLAYTIME -> Material.CLOCK
            QuestType.WALK -> Material.LEATHER_BOOTS
        }

        val lore = mutableListOf(
            "&7Type: &e${quest.type.name}",
            "&7Target: &f${quest.target ?: "None"}",
            "&7Required: &b${quest.requiredAmount}",
            "&7XP: &a${quest.rewardXp}",
            "&7Extra Rewards: &6${quest.extraRewards.size}",
            "",
            "&eLeft-Click: &7Edit",
            "&cShift + Right-Click: &7Delete"
        )

        return ItemBuilder(mat)
            .setName("&e${quest.displayName ?: quest.id}", player)
            .setLore(lore, player)
            .build()
    }

    fun openQuestEditMenu(player: Player, data: QuestEditorData) {
        val config = plugin.menuManager.getMenuConfig("editor_edit_menu.yml")
        val titleFormat = if (data.isNew) {
            config.getString("menu.title_create", "&8Create Quest: %id%")!!
        } else {
            config.getString("menu.title_edit", "&8Edit Quest: %id%")!!
        }
        val displayId = if (data.id.isBlank()) "new" else data.id
        val title = titleFormat.replace("%id%", displayId)
        val size = config.getInt("menu.size", 54)

        val inv = Bukkit.createInventory(
            EditorInventoryHolder(EditorMenuType.QUEST_EDIT, data),
            size,
            ColorUtils.colorize(player, title)
        )

        val bgSection = config.getConfigurationSection("menu.items.background")
        if (bgSection != null) {
            val bgMat = bgSection.getString("material", "GRAY_STAINED_GLASS_PANE")!!
            val bgSlots = bgSection.getIntegerList("slots")
            val bgItem = ItemBuilder.fromIdentifier(bgMat).setName(" ").build()
            for (s in bgSlots) {
                if (s in 0 until size) inv.setItem(s, bgItem)
            }
        }

        val idSlot = config.getInt("menu.items.id.slot", 11)
        val idMat = config.getString("menu.items.id.material", "OAK_SIGN")!!
        val idName = config.getString("menu.items.id.name", "&fQuest ID")!!
        val idLore = if (data.isNew) {
            listOf("&7Current: &f${data.id.ifBlank { "Not set" }}", "", "&eClick to set ID.")
        } else {
            listOf("&7ID: &f${data.id}")
        }
        inv.setItem(idSlot, ItemBuilder.fromIdentifier(idMat).setName(idName, player).setLore(idLore, player).build())

        val nameSlot = config.getInt("menu.items.display_name.slot", 12)
        val nameMat = config.getString("menu.items.display_name.material", "NAME_TAG")!!
        val nameName = config.getString("menu.items.display_name.name", "&bDisplay Name")!!
        inv.setItem(
            nameSlot,
            ItemBuilder.fromIdentifier(nameMat)
                .setName(nameName, player)
                .setLore(listOf("&7Current: &f${data.displayName.ifBlank { "None" }}", "", "&eClick to change."), player)
                .build()
        )

        val nextType = getNextQuestType(data.type)
        val typeSlot = config.getInt("menu.items.type.slot", 13)
        val typeMat = config.getString("menu.items.type.material", "PAPER")!!
        val typeName = config.getString("menu.items.type.name", "&eQuest Type")!!
        inv.setItem(
            typeSlot,
            ItemBuilder.fromIdentifier(typeMat)
                .setName(typeName, player)
                .setLore(listOf("&7Current: &a&l${data.type.name}", "&7Next: &8${nextType.name}", "", "&eClick to switch type."), player)
                .build()
        )

        val targetSlot = config.getInt("menu.items.target.slot", 14)
        val targetMat = config.getString("menu.items.target.material", "TARGET")!!
        val targetName = config.getString("menu.items.target.name", "&cTarget")!!
        inv.setItem(
            targetSlot,
            ItemBuilder.fromIdentifier(targetMat)
                .setName(targetName, player)
                .setLore(listOf("&7Current: &f${data.target.ifBlank { "None" }}", "", "&eClick to change."), player)
                .build()
        )

        val amountSlot = config.getInt("menu.items.required_amount.slot", 15)
        val amountMat = config.getString("menu.items.required_amount.material", "REPEATER")!!
        val amountName = config.getString("menu.items.required_amount.name", "&aRequired Amount")!!
        inv.setItem(
            amountSlot,
            ItemBuilder.fromIdentifier(amountMat)
                .setName(amountName, player)
                .setLore(listOf("&7Current: &b${data.requiredAmount}", "", "&eClick to change."), player)
                .build()
        )

        val xpSlot = config.getInt("menu.items.reward_xp.slot", 21)
        val xpMat = config.getString("menu.items.reward_xp.material", "EXPERIENCE_BOTTLE")!!
        val xpName = config.getString("menu.items.reward_xp.name", "&6Reward XP")!!
        inv.setItem(
            xpSlot,
            ItemBuilder.fromIdentifier(xpMat)
                .setName(xpName, player)
                .setLore(listOf("&7Current: &a${data.rewardXp} XP", "", "&eClick to change."), player)
                .build()
        )

        val rewardsSlot = config.getInt("menu.items.extra_rewards.slot", 23)
        val rewardsMat = config.getString("menu.items.extra_rewards.material", "CHEST")!!
        val rewardsName = config.getString("menu.items.extra_rewards.name", "&dExtra Rewards")!!
        val rewardLore = mutableListOf<String>()
        if (data.extraRewards.isEmpty()) {
            rewardLore.add("&8(None)")
        } else {
            data.extraRewards.forEach { rewardLore.add("&8- &f$it") }
        }
        rewardLore.add("")
        rewardLore.add("&aLeft-Click: &7Add reward")
        rewardLore.add("&cRight-Click: &7Clear all")
        inv.setItem(rewardsSlot, ItemBuilder.fromIdentifier(rewardsMat).setName(rewardsName, player).setLore(rewardLore, player).build())

        val backSlot = config.getInt("menu.items.back_button.slot", 45)
        val backMat = config.getString("menu.items.back_button.material", "ARROW")!!
        val backName = config.getString("menu.items.back_button.name", "&cBack")!!
        inv.setItem(backSlot, ItemBuilder.fromIdentifier(backMat).setName(backName, player).build())

        val saveSlot = config.getInt("menu.items.save_button.slot", 49)
        val saveMat = config.getString("menu.items.save_button.material", "EMERALD_BLOCK")!!
        val saveName = config.getString("menu.items.save_button.name", "&a&lSave Quest")!!
        inv.setItem(saveSlot, ItemBuilder.fromIdentifier(saveMat).setName(saveName, player).build())

        if (!data.isNew) {
            val delSlot = config.getInt("menu.items.delete_button.slot", 53)
            val delMat = config.getString("menu.items.delete_button.material", "REDSTONE_BLOCK")!!
            val delName = config.getString("menu.items.delete_button.name", "&c&lDelete Quest")!!
            inv.setItem(delSlot, ItemBuilder.fromIdentifier(delMat).setName(delName, player).build())
        }

        openEditorMenus[player.uniqueId] = Pair(EditorMenuType.QUEST_EDIT, data)
        player.openInventory(inv)
    }

    fun getNextQuestType(current: QuestType): QuestType {
        val values = QuestType.values()
        val nextOrdinal = (current.ordinal + 1) % values.size
        return values[nextOrdinal]
    }

    fun promptInput(player: Player, data: QuestEditorData, inputType: EditorInputType, promptText: String) {
        player.closeInventory()
        startSession(player, EditorSession(data, inputType, promptText))

        val prefix = plugin.configManager.config.getString("settings.prefix", "&8[&bFrPass&8] ") ?: "&8[&bFrPass&8] "
        player.sendMessage(ColorUtils.colorize("$prefix&e$promptText &7(Type &ccancel &7to abort)"))
    }

    fun applySessionInput(player: Player, session: EditorSession, input: String) {
        endSession(player)
        val trimmed = input.trim()

        plugin.foliaLib.impl.runNextTick {
            if (trimmed.equals("cancel", ignoreCase = true)) {
                player.sendMessage(ColorUtils.colorize("&cCancelled."))
                openQuestEditMenu(player, session.data)
                return@runNextTick
            }

            val data = session.data
            when (session.inputType) {
                EditorInputType.ID -> {
                    val formattedId = trimmed.lowercase().replace(" ", "_")
                    data.id = formattedId
                }
                EditorInputType.DISPLAY_NAME -> {
                    data.displayName = trimmed
                }
                EditorInputType.TARGET -> {
                    val target = if (trimmed.equals("none", ignoreCase = true) || trimmed.equals("clear", ignoreCase = true)) "" else trimmed.uppercase()
                    data.target = target
                }
                EditorInputType.REQUIRED_AMOUNT -> {
                    val amount = trimmed.toIntOrNull()
                    if (amount != null && amount > 0) {
                        data.requiredAmount = amount
                    }
                }
                EditorInputType.REWARD_XP -> {
                    val xp = trimmed.toIntOrNull()
                    if (xp != null && xp >= 0) {
                        data.rewardXp = xp
                    }
                }
                EditorInputType.ADD_EXTRA_REWARD -> {
                    data.extraRewards.add(trimmed)
                }
            }

            openQuestEditMenu(player, data)
        }
    }
}

open class EditorInventoryHolder(val type: QuestEditorManager.EditorMenuType, val data: Any) : InventoryHolder {
    override fun getInventory(): Inventory = error("Editor GUI")
}
