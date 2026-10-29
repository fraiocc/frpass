package cc.fraio.frpass.menus

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.ItemBuilder
import cc.fraio.frpass.utils.msg
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

class QuestsMenu(private val plugin: FrPass) {

    fun open(player: Player, page: Int = 1) {
        val config = plugin.menuManager.getMenuConfig("quests_menu.yml")
        val questSlots = config.getIntegerList("menu.quest-slots")
        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
        val activeQuests = data.activeQuests.mapNotNull { plugin.questManager.quests[it] }
        val pageSize = questSlots.size.coerceAtLeast(1)
        val totalPages = ((activeQuests.size - 1) / pageSize + 1).coerceAtLeast(1)
        val currentPage = page.coerceIn(1, totalPages)

        val titleRaw = config.getString("menu.title", "&8Quests Menu (Page %page%)")!!
            .replace("%page%", currentPage.toString())
            .replace("%total%", totalPages.toString())
        val title = ColorUtils.colorize(player, titleRaw)
        val size = config.getInt("menu.size", 54)

        val inv = Bukkit.createInventory(MenuInventoryHolder(MenuManager.MenuType.QUESTS, currentPage), size, title)

        val itemsSection = config.getConfigurationSection("menu.items")
        val bgMatStr = itemsSection?.getString("background.material", "GRAY_STAINED_GLASS_PANE") ?: "GRAY_STAINED_GLASS_PANE"
        val bgModelData = itemsSection?.getInt("background.custom-model-data", 0) ?: 0
        val bgItem = ItemBuilder.fromIdentifier(bgMatStr, Material.GRAY_STAINED_GLASS_PANE)
            .setName(" ", player)
            .setCustomModelData(bgModelData)
            .build()

        if (itemsSection != null) {
            for (key in itemsSection.getKeys(false)) {
                val keyName = key.lowercase()
                val slots = itemsSection.getIntegerList("$key.slots")

                if (keyName == "prev_page" && currentPage <= 1) {
                    for (s in slots) {
                        if (s in 0 until size) inv.setItem(s, bgItem)
                    }
                    continue
                }
                if (keyName == "next_page" && currentPage >= totalPages) {
                    for (s in slots) {
                        if (s in 0 until size) inv.setItem(s, bgItem)
                    }
                    continue
                }

                val materialStr = itemsSection.getString("$key.material", "STONE")!!
                val name = itemsSection.getString("$key.name", "")!!
                val lore = itemsSection.getStringList("$key.lore")
                val modelData = itemsSection.getInt("$key.custom-model-data", 0)

                val item = ItemBuilder.fromIdentifier(materialStr, Material.STONE)
                    .setName(name, player)
                    .setLore(lore, player)
                    .setCustomModelData(modelData)
                    .build()

                for (slot in slots) {
                    if (slot in 0 until size) {
                        inv.setItem(slot, item)
                    }
                }
            }
        }

        val rerollEnabled = plugin.configManager.config.getBoolean("reroll.enabled", true)
        if (rerollEnabled) {
            val reqPerm = plugin.configManager.config.getBoolean("reroll.require-permission", false)
            val perm = plugin.configManager.config.getString("reroll.permission", "frpass.reroll") ?: "frpass.reroll"
            if (!reqPerm || player.hasPermission(perm)) {
                val isRerollActive = plugin.menuManager.isRerollSessionActive(player)
                val selectedQuests = plugin.menuManager.getRerollSelection(player) ?: emptySet()
                val limit = plugin.configManager.config.getInt("reroll.limit", 3)

                val matStr = plugin.configManager.config.getString("reroll.button.material", "SUNFLOWER") ?: "SUNFLOWER"
                val modelData = plugin.configManager.config.getInt("reroll.button.custom-model-data", 0)
                val bName = plugin.configManager.config.getString("reroll.button.name", "&e&lQuest Reroll") ?: "&e&lQuest Reroll"
                val rawLore = if (isRerollActive) {
                    plugin.configManager.config.getStringList("reroll.button.lore-mode-active")
                } else {
                    plugin.configManager.config.getStringList("reroll.button.lore-normal")
                }

                val formattedLore = rawLore.map { line ->
                    line.replace("%limit%", limit.toString())
                        .replace("%selected%", selectedQuests.size.toString())
                }

                val builder = ItemBuilder.fromIdentifier(matStr, Material.SUNFLOWER)
                    .setName(bName, player)
                    .setLore(formattedLore, player)
                    .setCustomModelData(modelData)

                if (isRerollActive) {
                    builder.setGlow(true)
                }

                val rerollSlot = plugin.menuManager.resolveRerollSlot(size, config)
                inv.setItem(rerollSlot, builder.build())
            }
        }

        val startIndex = (currentPage - 1) * questSlots.size
        val endIndex = minOf(startIndex + questSlots.size, activeQuests.size)
        val isRerollSessionActive = plugin.menuManager.isRerollSessionActive(player)
        val selectedSet = plugin.menuManager.getRerollSelection(player) ?: emptySet()

        var slotIndex = 0
        for (i in startIndex until endIndex) {
            val quest = activeQuests[i]
            val progress = data.questProgress[quest.id] ?: 0
            val isCompleted = progress >= quest.requiredAmount
            val isSelected = selectedSet.contains(quest.id)

            val defaultMat = if (isCompleted) Material.MINECART else Material.PAPER
            val customMatStr = quest.material ?: defaultMat.name

            val qName = player.msg("gui.quest-item.name", "%quest%" to (quest.displayName ?: quest.id))
            val qLore = mutableListOf(
                player.msg("gui.quest-item.lore-type", "%type%" to quest.type.name),
                player.msg("gui.quest-item.lore-progress", "%progress%" to progress.toString(), "%required%" to quest.requiredAmount.toString()),
                player.msg("gui.quest-item.lore-reward", "%xp%" to quest.rewardXp.toString())
            )
            if (isCompleted) qLore.add(player.msg("gui.quest-item.lore-completed"))

            if (isRerollSessionActive) {
                if (isSelected) {
                    qLore.add(player.msg("gui.quest-item.lore-reroll-selected"))
                } else {
                    qLore.add(player.msg("gui.quest-item.lore-reroll-unselected"))
                }
            }

            val builder = ItemBuilder.fromIdentifier(customMatStr, defaultMat)
                .setName(qName, player)
                .setLore(qLore, player)
                .setCustomModelData(quest.customModelData)
            if (isSelected) {
                builder.setGlow(true)
            }

            val targetSlot = questSlots[slotIndex]
            if (targetSlot in 0 until size) {
                inv.setItem(targetSlot, builder.build())
            }
            slotIndex++
        }

        player.openInventory(inv)
        plugin.menuManager.setOpenMenu(player, MenuManager.MenuType.QUESTS, currentPage)
    }
}
