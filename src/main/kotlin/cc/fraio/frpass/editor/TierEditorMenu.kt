package cc.fraio.frpass.editor

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.menus.MenuSupport
import cc.fraio.frpass.model.Tier
import cc.fraio.frpass.utils.ItemBuilder
import cc.fraio.frpass.utils.msg
import org.bukkit.Material
import org.bukkit.entity.Player

class TierEditorMenu(private val plugin: FrPass, private val manager: EditorManager) {

    fun getSortedTiers(): List<Tier> = plugin.tierManager.tiers.values.sortedBy { it.level }

    fun getListContentSlots(): List<Int> {
        val config = plugin.menuManager.getMenuConfig("editor_tier_list_menu.yml")
        return config.getIntegerList("menu.tier-slots").ifEmpty {
            listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43)
        }
    }

    fun getListTotalPages(): Int {
        val count = plugin.tierManager.tiers.size
        val pageSize = getListContentSlots().size.coerceAtLeast(1)
        return ((count - 1) / pageSize + 1).coerceAtLeast(1)
    }

    fun openList(player: Player, page: Int = 1) {
        val config = plugin.menuManager.getMenuConfig("editor_tier_list_menu.yml")
        val tiers = getSortedTiers()
        val tierSlots = getListContentSlots()
        val pageSize = tierSlots.size.coerceAtLeast(1)
        val totalPages = ((tiers.size - 1) / pageSize + 1).coerceAtLeast(1)
        val currentPage = page.coerceIn(1, totalPages)

        val titleRaw = config.getString("menu.title", "&8Tier Editor (Page %page%/%total%)")!!
            .replace("%page%", currentPage.toString())
            .replace("%total%", totalPages.toString())
        val size = config.getInt("menu.size", 54)
        val inv = MenuSupport.createInventory(EditorInventoryHolder(EditorManager.MenuType.TIER_LIST, currentPage), size, player, titleRaw)

        val itemsSection = config.getConfigurationSection("menu.items")
        if (itemsSection != null) {
            for (key in itemsSection.getKeys(false)) {
                val keyName = key.lowercase()
                if ((keyName == "prev_page" && currentPage <= 1) || (keyName == "next_page" && currentPage >= totalPages)) continue
                MenuSupport.placeSectionItem(inv, itemsSection, key, player, size)
            }
        }

        val rawName = config.getString("menu.tier-item.name", "&aTier %tier%") ?: "&aTier %tier%"
        val rawLore = config.getStringList("menu.tier-item.lore").ifEmpty {
            listOf("&7Required XP: &b%xp%", "&7Free Rewards: &a%free%", "&7Premium Rewards: &e%premium%", "", "&eLeft-Click: &7Edit", "&cShift + Right-Click: &7Delete")
        }

        val pageTiers = tiers.drop((currentPage - 1) * pageSize).take(pageSize)
        for (i in pageTiers.indices) {
            val tier = pageTiers[i]
            val slot = tierSlots[i]
            if (slot !in 0 until size) continue
            val mat = tier.customIcon?.takeUnless { it.equals("default", true) } ?: "CHEST_MINECART"
            val name = tier.customName ?: rawName.replace("%tier%", tier.level.toString())
            val lore = rawLore.map { line ->
                line.replace("%xp%", tier.requiredXp.toString())
                    .replace("%free%", tier.freeRewards.size.toString())
                    .replace("%premium%", tier.premiumRewards.size.toString())
            }
            val item = ItemBuilder.fromIdentifier(mat, Material.CHEST_MINECART)
                .setName(name, player)
                .setLore(lore, player)
                .setCustomModelData(tier.customModelData ?: 0)
                .build()
            inv.setItem(slot, item)
        }

        player.openInventory(inv)
        manager.trackOpenMenu(player, EditorManager.MenuType.TIER_LIST, currentPage)
    }

    fun openEdit(player: Player, data: TierEditorData) {
        val config = plugin.menuManager.getMenuConfig("editor_tier_edit_menu.yml")
        val titleFormat = if (data.isNew) {
            config.getString("menu.title_create", "&8Create Tier: %tier%")!!
        } else {
            config.getString("menu.title_edit", "&8Edit Tier: %tier%")!!
        }
        val size = config.getInt("menu.size", 54)
        val inv = MenuSupport.createInventory(
            EditorInventoryHolder(EditorManager.MenuType.TIER_EDIT, data),
            size,
            player,
            titleFormat.replace("%tier%", data.level.toString())
        )

        EditorSupport.fillBackground(inv, config, size)

        val noneText = config.getString("menu.placeholders.none", "None") ?: "None"
        val emptyRewardsText = config.getString("menu.placeholders.empty-rewards", "&8(None)") ?: "&8(None)"
        val rewardEntryFormat = config.getString("menu.placeholders.reward-entry", "&8- &f%reward%") ?: "&8- &f%reward%"

        val previewSlot = config.getInt("menu.items.preview.slot", 4)
        if (previewSlot in 0 until size) {
            val previewMat = data.customIcon?.takeUnless { it.equals("default", true) } ?: "CHEST_MINECART"
            val previewName = data.customName ?: player.msg("gui.tier-item.name", "%tier%" to data.level.toString())
            val previewLore = listOf(
                player.msg("gui.tier-item.lore-xp", "%xp%" to data.requiredXp.toString()),
                "&7Free: &a${data.freeRewards.size} &8| &7Premium: &e${data.premiumRewards.size}"
            )
            inv.setItem(
                previewSlot,
                ItemBuilder.fromIdentifier(previewMat, Material.CHEST_MINECART)
                    .setName(previewName, player)
                    .setLore(previewLore, player)
                    .setCustomModelData(data.customModelData)
                    .build()
            )
        }

        val levelSlot = config.getInt("menu.items.level.slot", 11)
        if (levelSlot in 0 until size) {
            val levelLoreRaw = if (data.isNew) {
                config.getStringList("menu.items.level.lore-new").ifEmpty { listOf("&7Current: &f%tier%", "", "&eClick to set level.") }
            } else {
                config.getStringList("menu.items.level.lore-edit").ifEmpty { listOf("&7Level: &f%tier%") }
            }
            inv.setItem(
                levelSlot,
                ItemBuilder.fromIdentifier(config.getString("menu.items.level.material", "OAK_SIGN")!!)
                    .setName(config.getString("menu.items.level.name", "&fTier Level")!!, player)
                    .setLore(levelLoreRaw.map { it.replace("%tier%", data.level.toString()) }, player)
                    .build()
            )
        }

        val nameSlot = config.getInt("menu.items.display_name.slot", 12)
        if (nameSlot in 0 until size) {
            val nameLore = config.getStringList("menu.items.display_name.lore").ifEmpty {
                listOf("&7Current: &f%display_name%", "", "&aLeft-Click: &7Change", "&cRight-Click: &7Reset")
            }.map { it.replace("%display_name%", data.customName ?: noneText) }
            inv.setItem(
                nameSlot,
                ItemBuilder.fromIdentifier(config.getString("menu.items.display_name.material", "NAME_TAG")!!)
                    .setName(config.getString("menu.items.display_name.name", "&bDisplay Name")!!, player)
                    .setLore(nameLore, player)
                    .build()
            )
        }

        val xpSlot = config.getInt("menu.items.required_xp.slot", 13)
        if (xpSlot in 0 until size) {
            val xpLore = config.getStringList("menu.items.required_xp.lore").ifEmpty {
                listOf("&7Current: &b%xp% XP", "", "&aLeft-Click: &7Enter value", "&eRight-Click: &7+100", "&cShift-Right-Click: &7-100")
            }.map { it.replace("%xp%", data.requiredXp.toString()) }
            inv.setItem(
                xpSlot,
                ItemBuilder.fromIdentifier(config.getString("menu.items.required_xp.material", "EXPERIENCE_BOTTLE")!!)
                    .setName(config.getString("menu.items.required_xp.name", "&6Required XP")!!, player)
                    .setLore(xpLore, player)
                    .build()
            )
        }

        val iconSlot = config.getInt("menu.items.icon.slot", 14)
        if (iconSlot in 0 until size) {
            val iconMat = data.customIcon ?: config.getString("menu.items.icon.material", "ITEM_FRAME")!!
            val iconLore = config.getStringList("menu.items.icon.lore").ifEmpty {
                listOf("&7Material: &f%material%", "&7Model Data: &f%model_data%", "", "&bClick with item: &7Copy item", "&aLeft-Click: &7Set Icon Material", "&eRight-Click: &7Set Model Data", "&cShift-Right-Click: &7Reset Icon")
            }.map { it.replace("%material%", data.customIcon ?: noneText).replace("%model_data%", data.customModelData.toString()) }
            inv.setItem(
                iconSlot,
                ItemBuilder.fromIdentifier(iconMat, Material.ITEM_FRAME)
                    .setName(config.getString("menu.items.icon.name", "&eTier Icon")!!, player)
                    .setLore(iconLore, player)
                    .setCustomModelData(data.customModelData)
                    .build()
            )
        }

        EditorSupport.placeRewardListItem(inv, config, "free_rewards", 21, "CHEST", "&aFree Rewards", data.freeRewards, emptyRewardsText, rewardEntryFormat, player, size)
        EditorSupport.placeRewardListItem(inv, config, "premium_rewards", 23, "ENDER_CHEST", "&ePremium Rewards", data.premiumRewards, emptyRewardsText, rewardEntryFormat, player, size)

        val backSlot = config.getInt("menu.items.back_button.slot", 45)
        if (backSlot in 0 until size) {
            inv.setItem(backSlot, ItemBuilder.fromIdentifier(config.getString("menu.items.back_button.material", "ARROW")!!).setName(config.getString("menu.items.back_button.name", "&cBack")!!, player).build())
        }
        val saveSlot = config.getInt("menu.items.save_button.slot", 49)
        if (saveSlot in 0 until size) {
            inv.setItem(saveSlot, ItemBuilder.fromIdentifier(config.getString("menu.items.save_button.material", "EMERALD_BLOCK")!!).setName(config.getString("menu.items.save_button.name", "&a&lSave Tier")!!, player).build())
        }
        if (!data.isNew) {
            val delSlot = config.getInt("menu.items.delete_button.slot", 53)
            if (delSlot in 0 until size) {
                inv.setItem(delSlot, ItemBuilder.fromIdentifier(config.getString("menu.items.delete_button.material", "REDSTONE_BLOCK")!!).setName(config.getString("menu.items.delete_button.name", "&c&lDelete Tier")!!, player).build())
            }
        }

        player.openInventory(inv)
        manager.trackOpenMenu(player, EditorManager.MenuType.TIER_EDIT, data)
    }
}
