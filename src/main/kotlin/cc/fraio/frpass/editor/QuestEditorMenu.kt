package cc.fraio.frpass.editor

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.menus.MenuSupport
import cc.fraio.frpass.quests.Quest
import cc.fraio.frpass.quests.QuestType
import cc.fraio.frpass.utils.ItemBuilder
import cc.fraio.frpass.utils.msg
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class QuestEditorMenu(private val plugin: FrPass, private val manager: EditorManager) {

    fun getListContentSlots(): List<Int> {
        val config = plugin.menuManager.getMenuConfig("editor_list_menu.yml")
        return config.getIntegerList("menu.quest-slots").ifEmpty {
            listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43)
        }
    }

    fun getListTotalPages(): Int {
        val count = plugin.questManager.quests.size
        val pageSize = getListContentSlots().size.coerceAtLeast(1)
        return ((count - 1) / pageSize + 1).coerceAtLeast(1)
    }

    fun getSortedQuests(): List<Quest> = plugin.questManager.quests.values.sortedBy { it.id }

    fun getAdjacentQuestType(current: QuestType, delta: Int): QuestType {
        val values = QuestType.values()
        return values[Math.floorMod(current.ordinal + delta, values.size)]
    }

    fun getDefaultMaterial(type: QuestType): Material = when (type) {
        QuestType.BREAK_BLOCK -> Material.DIAMOND_PICKAXE
        QuestType.PLACE_BLOCK -> Material.BRICKS
        QuestType.KILL_MOB -> Material.IRON_SWORD
        QuestType.KILL_PLAYER -> Material.DIAMOND_SWORD
        QuestType.FISH -> Material.FISHING_ROD
        QuestType.CRAFT -> Material.CRAFTING_TABLE
        QuestType.PLAYTIME -> Material.CLOCK
        QuestType.WALK -> Material.LEATHER_BOOTS
        QuestType.CONSUME -> Material.GOLDEN_APPLE
        QuestType.BREED_ANIMAL -> Material.WHEAT
        QuestType.ENCHANT_ITEM -> Material.ENCHANTING_TABLE
        QuestType.EXP_EARN -> Material.EXPERIENCE_BOTTLE
        QuestType.SHEAR_SHEEP -> Material.SHEARS
    }

    fun openList(player: Player, page: Int = 1) {
        val config = plugin.menuManager.getMenuConfig("editor_list_menu.yml")
        val quests = getSortedQuests()
        val questSlots = getListContentSlots()
        val pageSize = questSlots.size.coerceAtLeast(1)
        val totalPages = ((quests.size - 1) / pageSize + 1).coerceAtLeast(1)
        val currentPage = page.coerceIn(1, totalPages)

        val titleRaw = config.getString("menu.title", "&8Quest Editor (Page %page%/%total%)")!!
            .replace("%page%", currentPage.toString())
            .replace("%total%", totalPages.toString())
        val size = config.getInt("menu.size", 54)
        val inv = MenuSupport.createInventory(EditorInventoryHolder(EditorManager.MenuType.QUEST_LIST, currentPage), size, player, titleRaw)

        val itemsSection = config.getConfigurationSection("menu.items")
        if (itemsSection != null) {
            for (key in itemsSection.getKeys(false)) {
                val keyName = key.lowercase()
                if ((keyName == "prev_page" && currentPage <= 1) || (keyName == "next_page" && currentPage >= totalPages)) continue
                MenuSupport.placeSectionItem(inv, itemsSection, key, player, size)
            }
        }

        val pageQuests = quests.drop((currentPage - 1) * pageSize).take(pageSize)
        for (i in pageQuests.indices) {
            val slot = questSlots[i]
            if (slot in 0 until size) {
                inv.setItem(slot, createQuestDisplayItem(pageQuests[i], player))
            }
        }

        player.openInventory(inv)
        manager.trackOpenMenu(player, EditorManager.MenuType.QUEST_LIST, currentPage)
    }

    fun openEdit(player: Player, data: QuestEditorData) {
        val config = plugin.menuManager.getMenuConfig("editor_edit_menu.yml")
        val titleFormat = if (data.isNew) {
            config.getString("menu.title_create", "&8Create Quest: %id%")!!
        } else {
            config.getString("menu.title_edit", "&8Edit Quest: %id%")!!
        }
        val displayId = if (data.id.isBlank()) "new" else data.id
        val size = config.getInt("menu.size", 54)
        val inv = MenuSupport.createInventory(
            EditorInventoryHolder(EditorManager.MenuType.QUEST_EDIT, data),
            size,
            player,
            titleFormat.replace("%id%", displayId)
        )

        EditorSupport.fillBackground(inv, config, size)

        val previewSlot = config.getInt("menu.items.preview.slot", 4)
        if (previewSlot in 0 until size) {
            val defaultMat = getDefaultMaterial(data.type)
            val previewMat = data.material ?: defaultMat.name
            val previewLore = mutableListOf(
                player.msg("gui.quest-item.lore-type", "%type%" to data.type.name),
                player.msg("gui.quest-item.lore-progress", "%progress%" to "0", "%required%" to data.requiredAmount.toString()),
                player.msg("gui.quest-item.lore-reward", "%xp%" to data.rewardXp.toString())
            )
            if (data.target.isNotBlank()) previewLore.add(1, "&7Target: &f${data.target}")
            inv.setItem(
                previewSlot,
                ItemBuilder.fromIdentifier(previewMat, defaultMat)
                    .setName(player.msg("gui.quest-item.name", "%quest%" to data.displayName.ifBlank { displayId }), player)
                    .setLore(previewLore, player)
                    .setCustomModelData(data.customModelData)
                    .build()
            )
        }

        val notSetText = config.getString("menu.placeholders.not-set", "Not set") ?: "Not set"
        val noneText = config.getString("menu.placeholders.none", "None") ?: "None"
        val emptyRewardsText = config.getString("menu.placeholders.empty-rewards", "&8(None)") ?: "&8(None)"
        val rewardEntryFormat = config.getString("menu.placeholders.reward-entry", "&8- &f%reward%") ?: "&8- &f%reward%"

        val idSlot = config.getInt("menu.items.id.slot", 11)
        if (idSlot in 0 until size) {
            val idLoreRaw = if (data.isNew) {
                config.getStringList("menu.items.id.lore-new").ifEmpty { listOf("&7Current: &f%id%", "", "&eClick to set ID.") }
            } else {
                config.getStringList("menu.items.id.lore-edit").ifEmpty { listOf("&7ID: &f%id%") }
            }
            inv.setItem(
                idSlot,
                ItemBuilder.fromIdentifier(config.getString("menu.items.id.material", "OAK_SIGN")!!)
                    .setName(config.getString("menu.items.id.name", "&fQuest ID")!!, player)
                    .setLore(idLoreRaw.map { it.replace("%id%", data.id.ifBlank { notSetText }) }, player)
                    .build()
            )
        }

        val nameSlot = config.getInt("menu.items.display_name.slot", 12)
        if (nameSlot in 0 until size) {
            val lore = config.getStringList("menu.items.display_name.lore").ifEmpty {
                listOf("&7Current: &f%display_name%", "", "&eClick to change.")
            }.map { it.replace("%display_name%", data.displayName.ifBlank { noneText }) }
            inv.setItem(
                nameSlot,
                ItemBuilder.fromIdentifier(config.getString("menu.items.display_name.material", "NAME_TAG")!!)
                    .setName(config.getString("menu.items.display_name.name", "&bDisplay Name")!!, player)
                    .setLore(lore, player)
                    .build()
            )
        }

        val nextType = getAdjacentQuestType(data.type, 1)
        val prevType = getAdjacentQuestType(data.type, -1)
        val typeSlot = config.getInt("menu.items.type.slot", 13)
        if (typeSlot in 0 until size) {
            val lore = config.getStringList("menu.items.type.lore").ifEmpty {
                listOf("&7Current: &a&l%type%", "&7Next: &8%next_type% &8| &7Prev: &8%prev_type%", "", "&aLeft-Click: &7Next type", "&eRight-Click: &7Previous type")
            }.map { it.replace("%type%", data.type.name).replace("%next_type%", nextType.name).replace("%prev_type%", prevType.name) }
            inv.setItem(
                typeSlot,
                ItemBuilder.fromIdentifier(config.getString("menu.items.type.material", "PAPER")!!)
                    .setName(config.getString("menu.items.type.name", "&eQuest Type")!!, player)
                    .setLore(lore, player)
                    .build()
            )
        }

        val targetSlot = config.getInt("menu.items.target.slot", 14)
        if (targetSlot in 0 until size) {
            val lore = config.getStringList("menu.items.target.lore").ifEmpty {
                listOf("&7Current: &f%target%", "", "&aLeft-Click: &7Set target", "&cRight-Click: &7Clear target")
            }.map { it.replace("%target%", data.target.ifBlank { noneText }) }
            inv.setItem(
                targetSlot,
                ItemBuilder.fromIdentifier(config.getString("menu.items.target.material", "TARGET")!!)
                    .setName(config.getString("menu.items.target.name", "&cTarget")!!, player)
                    .setLore(lore, player)
                    .build()
            )
        }

        val amountSlot = config.getInt("menu.items.required_amount.slot", 15)
        if (amountSlot in 0 until size) {
            val lore = config.getStringList("menu.items.required_amount.lore").ifEmpty {
                listOf("&7Current: &b%required%", "", "&aLeft-Click: &7Enter value", "&eRight-Click: &7+10", "&cShift-Right-Click: &7-10")
            }.map { it.replace("%required%", data.requiredAmount.toString()) }
            inv.setItem(
                amountSlot,
                ItemBuilder.fromIdentifier(config.getString("menu.items.required_amount.material", "REPEATER")!!)
                    .setName(config.getString("menu.items.required_amount.name", "&aRequired Amount")!!, player)
                    .setLore(lore, player)
                    .build()
            )
        }

        val xpSlot = config.getInt("menu.items.reward_xp.slot", 21)
        if (xpSlot in 0 until size) {
            val lore = config.getStringList("menu.items.reward_xp.lore").ifEmpty {
                listOf("&7Current: &a%xp% XP", "", "&aLeft-Click: &7Enter value", "&eRight-Click: &7+50", "&cShift-Right-Click: &7-50")
            }.map { it.replace("%xp%", data.rewardXp.toString()) }
            inv.setItem(
                xpSlot,
                ItemBuilder.fromIdentifier(config.getString("menu.items.reward_xp.material", "EXPERIENCE_BOTTLE")!!)
                    .setName(config.getString("menu.items.reward_xp.name", "&6Reward XP")!!, player)
                    .setLore(lore, player)
                    .build()
            )
        }

        val iconSlot = config.getInt("menu.items.icon.slot", 22)
        if (iconSlot in 0 until size) {
            val iconMat = data.material ?: config.getString("menu.items.icon.material", "ITEM_FRAME")!!
            val lore = config.getStringList("menu.items.icon.lore").ifEmpty {
                listOf("&7Material: &f%material%", "&7Model Data: &f%model_data%", "", "&bClick with item: &7Copy item", "&aLeft-Click: &7Set Icon Material", "&eRight-Click: &7Set Model Data", "&cShift-Right-Click: &7Reset Icon")
            }.map { it.replace("%material%", data.material ?: noneText).replace("%model_data%", data.customModelData.toString()) }
            inv.setItem(
                iconSlot,
                ItemBuilder.fromIdentifier(iconMat, Material.ITEM_FRAME)
                    .setName(config.getString("menu.items.icon.name", "&eQuest Icon")!!, player)
                    .setLore(lore, player)
                    .setCustomModelData(data.customModelData)
                    .build()
            )
        }

        EditorSupport.placeRewardListItem(inv, config, "extra_rewards", 23, "CHEST", "&dExtra Rewards", data.extraRewards, emptyRewardsText, rewardEntryFormat, player, size)

        val backSlot = config.getInt("menu.items.back_button.slot", 45)
        if (backSlot in 0 until size) {
            inv.setItem(backSlot, ItemBuilder.fromIdentifier(config.getString("menu.items.back_button.material", "ARROW")!!).setName(config.getString("menu.items.back_button.name", "&cBack")!!, player).build())
        }
        val saveSlot = config.getInt("menu.items.save_button.slot", 49)
        if (saveSlot in 0 until size) {
            inv.setItem(saveSlot, ItemBuilder.fromIdentifier(config.getString("menu.items.save_button.material", "EMERALD_BLOCK")!!).setName(config.getString("menu.items.save_button.name", "&a&lSave Quest")!!, player).build())
        }
        if (!data.isNew) {
            val delSlot = config.getInt("menu.items.delete_button.slot", 53)
            if (delSlot in 0 until size) {
                inv.setItem(delSlot, ItemBuilder.fromIdentifier(config.getString("menu.items.delete_button.material", "REDSTONE_BLOCK")!!).setName(config.getString("menu.items.delete_button.name", "&c&lDelete Quest")!!, player).build())
            }
        }

        player.openInventory(inv)
        manager.trackOpenMenu(player, EditorManager.MenuType.QUEST_EDIT, data)
    }

    private fun createQuestDisplayItem(quest: Quest, player: Player): ItemStack {
        val config = plugin.menuManager.getMenuConfig("editor_list_menu.yml")
        val defaultMat = getDefaultMaterial(quest.type)
        val targetNone = config.getString("menu.quest-item.target-none", "None") ?: "None"
        val rawName = config.getString("menu.quest-item.name", "&e%quest%") ?: "&e%quest%"
        val rawLore = config.getStringList("menu.quest-item.lore").ifEmpty {
            listOf("&7Type: &e%type%", "&7Target: &f%target%", "&7Required: &b%required%", "&7XP: &a%xp%", "&7Extra Rewards: &6%rewards%", "", "&eLeft-Click: &7Edit", "&bRight-Click: &7Duplicate", "&cShift + Right-Click: &7Delete")
        }

        val formattedName = rawName.replace("%quest%", quest.displayName ?: quest.id)
        val formattedLore = rawLore.map { line ->
            line.replace("%type%", quest.type.name)
                .replace("%target%", quest.target ?: targetNone)
                .replace("%required%", quest.requiredAmount.toString())
                .replace("%xp%", quest.rewardXp.toString())
                .replace("%rewards%", quest.extraRewards.size.toString())
        }

        return ItemBuilder.fromIdentifier(quest.material ?: defaultMat.name, defaultMat)
            .setName(formattedName, player)
            .setLore(formattedLore, player)
            .setCustomModelData(quest.customModelData)
            .build()
    }
}
