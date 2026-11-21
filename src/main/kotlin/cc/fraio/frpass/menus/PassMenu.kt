package cc.fraio.frpass.menus

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.model.Tier
import cc.fraio.frpass.utils.ItemBuilder
import cc.fraio.frpass.utils.msg
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

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
        val size = menuConfig.getInt("menu.size", 27)
        val inv = MenuSupport.createInventory(MenuInventoryHolder(MenuManager.MenuType.PASS, currentPage), size, player, titleRaw)

        val itemsSection = menuConfig.getConfigurationSection("menu.items")
        val bgItem = MenuSupport.buildBackgroundItem(itemsSection, player)
        val unclaimedCount = plugin.playerDataManager.getUnclaimedRewardCount(player.uniqueId).toString()

        itemsSection?.getKeys(false)?.forEach { key ->
            val keyName = key.lowercase()
            val slots = itemsSection.getIntegerList("$key.slots")

            if ((keyName == "prev_page" && currentPage <= 1) || (keyName == "next_page" && currentPage >= totalPages)) {
                slots.forEach { s -> if (s in 0 until size) inv.setItem(s, bgItem) }
                return@forEach
            }

            MenuSupport.placeSectionItem(inv, itemsSection, key, player, size) {
                it.replace("%count%", unclaimedCount)
            }
        }

        val freeSlots = menuConfig.getIntegerList("menu.free-tier-slots")
        val premiumSlots = menuConfig.getIntegerList("menu.premium-tier-slots")
        val data = plugin.playerDataManager.getPlayer(player.uniqueId)

        if (data != null) {
            val count = minOf(freeSlots.size, premiumSlots.size)
            var tierIndex = 1 + (currentPage - 1) * count

            val cfg = plugin.configManager.config
            val icons = IconConfig(
                claimedMat = cfg.getString("tier-icons.claimed.material", "MINECART")!!,
                claimedModel = cfg.getInt("tier-icons.claimed.custom-model-data", 0),
                unlockedMat = cfg.getString("tier-icons.unlocked.material", "CHEST_MINECART")!!,
                unlockedModel = cfg.getInt("tier-icons.unlocked.custom-model-data", 0),
                lockedMat = cfg.getString("tier-icons.locked.material", "HOPPER_MINECART")!!,
                lockedModel = cfg.getInt("tier-icons.locked.custom-model-data", 0)
            )
            val freeSuffix = player.msg("gui.tier-item.free-suffix")
            val premiumSuffix = player.msg("gui.tier-item.premium-suffix")

            for (i in 0 until count) {
                val tier = plugin.tierManager.getTier(tierIndex)
                if (tier != null) {
                    val isUnlocked = data.level >= tier.level
                    val freeItem = buildTierItem(player, tier, icons, freeSuffix, isUnlocked, data.claimedTiers.contains(tier.level), requiresPremiumNotice = false)
                    val premItem = buildTierItem(player, tier, icons, premiumSuffix, isUnlocked && data.premium, data.claimedPremiumTiers.contains(tier.level), requiresPremiumNotice = !data.premium)

                    val fSlot = freeSlots[i]
                    if (fSlot in 0 until size) inv.setItem(fSlot, freeItem)
                    val pSlot = premiumSlots[i]
                    if (pSlot in 0 until size) inv.setItem(pSlot, premItem)
                }
                tierIndex++
            }
        }

        player.openInventory(inv)
        plugin.menuManager.setOpenMenu(player, MenuManager.MenuType.PASS, currentPage)
    }

    private fun buildTierItem(
        player: Player,
        tier: Tier,
        icons: IconConfig,
        suffix: String,
        unlocked: Boolean,
        claimed: Boolean,
        requiresPremiumNotice: Boolean
    ): ItemStack {
        val customMat = tier.customIcon?.takeUnless { it.equals("default", true) }
        val matStr = customMat ?: when {
            claimed -> icons.claimedMat
            unlocked -> icons.unlockedMat
            else -> icons.lockedMat
        }
        val modelData = tier.customModelData ?: when {
            claimed -> icons.claimedModel
            unlocked -> icons.unlockedModel
            else -> icons.lockedModel
        }
        val statusText = when {
            claimed -> player.msg("gui.tier-item.status-claimed")
            unlocked -> player.msg("gui.tier-item.status-unlocked")
            else -> player.msg("gui.tier-item.status-locked")
        }
        val name = (tier.customName ?: player.msg("gui.tier-item.name", "%tier%" to tier.level.toString())) + suffix
        val lore = mutableListOf<String>()
        if (tier.customLore != null) {
            lore.addAll(tier.customLore)
        } else {
            lore.add(player.msg("gui.tier-item.lore-xp", "%xp%" to tier.requiredXp.toString()))
        }
        if (requiresPremiumNotice) {
            lore.add("")
            lore.add(player.msg("gui.tier-item.requires-premium"))
        }
        lore.add("")
        lore.add(player.msg("gui.tier-item.status-prefix") + statusText)

        return ItemBuilder.fromIdentifier(matStr, Material.MINECART)
            .setName(name, player)
            .setLore(lore, player)
            .setCustomModelData(modelData)
            .build()
    }

    private data class IconConfig(
        val claimedMat: String,
        val claimedModel: Int,
        val unlockedMat: String,
        val unlockedModel: Int,
        val lockedMat: String,
        val lockedModel: Int
    )
}
