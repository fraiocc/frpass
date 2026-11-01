package cc.fraio.frpass.menus

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.ItemBuilder
import cc.fraio.frpass.utils.msg
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

class PassMenu(private val plugin: FrPass) {

    fun getTotalPages(): Int {
        val menuConfig = plugin.menuManager.getMenuConfig("pass_menu.yml")
        val freeSlots = menuConfig.getIntegerList("menu.free-tier-slots")
        val premiumSlots = menuConfig.getIntegerList("menu.premium-tier-slots")
        val count = minOf(freeSlots.size, premiumSlots.size).coerceAtLeast(1)
        val maxTier = plugin.tierManager.tiers.keys.maxOrNull() ?: 1
        return ((maxTier - 1) / count + 1).coerceAtLeast(1)
    }

    fun open(player: Player, page: Int = 1) {
        val menuConfig = plugin.menuManager.getMenuConfig("pass_menu.yml")
        val totalPages = getTotalPages()
        val currentPage = page.coerceIn(1, totalPages)

        val titleRaw = menuConfig.getString("menu.title", "&8Battlepass (Page %page%)")!!
            .replace("%page%", currentPage.toString())
            .replace("%total%", totalPages.toString())
        val title = ColorUtils.colorize(player, titleRaw)
        val size = menuConfig.getInt("menu.size", 27)

        val inv = Bukkit.createInventory(MenuInventoryHolder(MenuManager.MenuType.PASS, currentPage), size, title)

        val itemsSection = menuConfig.getConfigurationSection("menu.items")
        val bgMatStr = itemsSection?.getString("background.material", "GRAY_STAINED_GLASS_PANE") ?: "GRAY_STAINED_GLASS_PANE"
        val bgModelData = itemsSection?.getInt("background.custom-model-data", 0) ?: 0
        val bgItem = ItemBuilder.fromIdentifier(bgMatStr, Material.GRAY_STAINED_GLASS_PANE)
            .setName(" ", player)
            .setCustomModelData(bgModelData)
            .build()

        itemsSection?.getKeys(false)?.forEach { key ->
            val keyName = key.lowercase()
            val slots = itemsSection.getIntegerList("$key.slots")

            if (keyName == "prev_page" && currentPage <= 1) {
                slots.forEach { s -> if (s in 0 until size) inv.setItem(s, bgItem) }
                return@forEach
            }
            if (keyName == "next_page" && currentPage >= totalPages) {
                slots.forEach { s -> if (s in 0 until size) inv.setItem(s, bgItem) }
                return@forEach
            }

            val matStr = itemsSection.getString("$key.material", "STONE")!!
            val name = itemsSection.getString("$key.name", " ")!!
            val lore = itemsSection.getStringList("$key.lore")
            val modelData = itemsSection.getInt("$key.custom-model-data", 0)

            val item = ItemBuilder.fromIdentifier(matStr, Material.STONE)
                .setName(name, player)
                .setLore(lore, player)
                .setCustomModelData(modelData)
                .build()

            slots.forEach { slot ->
                if (slot in 0 until size) {
                    inv.setItem(slot, item)
                }
            }
        }

        val freeSlots = menuConfig.getIntegerList("menu.free-tier-slots")
        val premiumSlots = menuConfig.getIntegerList("menu.premium-tier-slots")
        val data = plugin.playerDataManager.getPlayer(player.uniqueId)

        if (data != null) {
            val count = minOf(freeSlots.size, premiumSlots.size)
            var tierIndex = 1 + (currentPage - 1) * count

            val defaultMatStrClaimed = plugin.configManager.config.getString("tier-icons.claimed.material", "MINECART")!!
            val defaultModelDataClaimed = plugin.configManager.config.getInt("tier-icons.claimed.custom-model-data", 0)
            val defaultMatStrUnlocked = plugin.configManager.config.getString("tier-icons.unlocked.material", "CHEST_MINECART")!!
            val defaultModelDataUnlocked = plugin.configManager.config.getInt("tier-icons.unlocked.custom-model-data", 0)
            val defaultMatStrLocked = plugin.configManager.config.getString("tier-icons.locked.material", "HOPPER_MINECART")!!
            val defaultModelDataLocked = plugin.configManager.config.getInt("tier-icons.locked.custom-model-data", 0)

            val freeSuffix = player.msg("gui.tier-item.free-suffix")
            val premiumSuffix = player.msg("gui.tier-item.premium-suffix")

            for (i in 0 until count) {
                val tier = plugin.tierManager.getTier(tierIndex)
                if (tier != null) {
                    val isUnlocked = data.level >= tier.level
                    val isFreeClaimed = data.claimedTiers.contains(tier.level)
                    val isPremiumClaimed = data.claimedPremiumTiers.contains(tier.level)

                    val hasCustomIcon = tier.customIcon != null && !tier.customIcon.equals("default", true)
                    val customMatStr = if (hasCustomIcon) tier.customIcon else null
                    val name = tier.customName ?: player.msg("gui.tier-item.name", "%tier%" to tier.level.toString())

                    run {
                        val slot = freeSlots[i]
                        val matStrSlot = customMatStr ?: if (isFreeClaimed) defaultMatStrClaimed else if (isUnlocked) defaultMatStrUnlocked else defaultMatStrLocked
                        val modelData = tier.customModelData ?: if (isFreeClaimed) defaultModelDataClaimed else if (isUnlocked) defaultModelDataUnlocked else defaultModelDataLocked

                        val statusText = if (isFreeClaimed) player.msg("gui.tier-item.status-claimed")
                        else if (isUnlocked) player.msg("gui.tier-item.status-unlocked")
                        else player.msg("gui.tier-item.status-locked")

                        val lore = mutableListOf<String>()
                        if (tier.customLore != null) {
                            lore.addAll(tier.customLore)
                        } else {
                            lore.add(player.msg("gui.tier-item.lore-xp", "%xp%" to tier.requiredXp.toString()))
                        }
                        lore.add("")
                        lore.add(player.msg("gui.tier-item.status-prefix") + statusText)

                        val item = ItemBuilder.fromIdentifier(matStrSlot, Material.MINECART)
                            .setName(name + freeSuffix, player)
                            .setLore(lore, player)
                            .setCustomModelData(modelData)
                            .build()
                        if (slot in 0 until size) inv.setItem(slot, item)
                    }

                    run {
                        val slot = premiumSlots[i]
                        val hasPremium = data.premium
                        val matStrSlot = customMatStr ?: if (isPremiumClaimed) defaultMatStrClaimed else if (isUnlocked && hasPremium) defaultMatStrUnlocked else defaultMatStrLocked
                        val modelData = tier.customModelData ?: if (isPremiumClaimed) defaultModelDataClaimed else if (isUnlocked && hasPremium) defaultModelDataUnlocked else defaultModelDataLocked

                        val statusText = if (isPremiumClaimed) player.msg("gui.tier-item.status-claimed")
                        else if (isUnlocked && hasPremium) player.msg("gui.tier-item.status-unlocked")
                        else player.msg("gui.tier-item.status-locked")

                        val lore = mutableListOf<String>()
                        if (tier.customLore != null) {
                            lore.addAll(tier.customLore)
                        } else {
                            lore.add(player.msg("gui.tier-item.lore-xp", "%xp%" to tier.requiredXp.toString()))
                        }
                        if (!hasPremium) {
                            lore.add("")
                            lore.add(player.msg("gui.tier-item.requires-premium"))
                        }
                        lore.add("")
                        lore.add(player.msg("gui.tier-item.status-prefix") + statusText)

                        val item = ItemBuilder.fromIdentifier(matStrSlot, Material.MINECART)
                            .setName(name + premiumSuffix, player)
                            .setLore(lore, player)
                            .setCustomModelData(modelData)
                            .build()
                        if (slot in 0 until size) inv.setItem(slot, item)
                    }
                }
                tierIndex++
            }
        }

        player.openInventory(inv)
        plugin.menuManager.setOpenMenu(player, MenuManager.MenuType.PASS, currentPage)
    }
}
