package cc.fraio.frpass.menus

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.ItemBuilder
import cc.fraio.frpass.utils.msg
import org.bukkit.Material
import org.bukkit.entity.Player

class PassMenu(private val plugin: FrPass) {

    fun open(player: Player, page: Int = 1) {
        val menuConfig = plugin.menuManager.getMenuConfig("pass_menu.yml")
        val titleRaw = menuConfig.getString("menu.title", "&8Battlepass (Page %page%)")!!.replace("%page%", page.toString())
        val title = ColorUtils.colorize(player, titleRaw)
        val size = menuConfig.getInt("menu.size", 27)

        val inv = org.bukkit.Bukkit.createInventory(null, size, title)

        val itemsSection = menuConfig.getConfigurationSection("menu.items")
        itemsSection?.getKeys(false)?.forEach { key ->
            val matStr = itemsSection.getString("$key.material", "STONE")!!
            val material = Material.matchMaterial(matStr) ?: Material.STONE
            val name = itemsSection.getString("$key.name", " ")!!
            val lore = itemsSection.getStringList("$key.lore")
            val modelData = itemsSection.getInt("$key.custom-model-data", 0)
            val slots = itemsSection.getIntegerList("$key.slots")

            val item = ItemBuilder(material)
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
            val count = Math.min(freeSlots.size, premiumSlots.size)
            var tierIndex = 1 + (page - 1) * count

            for (i in 0 until count) {
                val tier = plugin.tierManager.getTier(tierIndex)
                if (tier != null) {
                    val isUnlocked = data.level >= tier.level
                    val isFreeClaimed = data.claimedTiers.contains(tier.level)
                    val isPremiumClaimed = data.claimedPremiumTiers.contains(tier.level)

                    val defaultMatStrClaimed = plugin.configManager.config.getString("tier-icons.claimed.material", "MINECART")!!
                    val defaultModelDataClaimed = plugin.configManager.config.getInt("tier-icons.claimed.custom-model-data", 0)
                    val defaultMatStrUnlocked = plugin.configManager.config.getString("tier-icons.unlocked.material", "CHEST_MINECART")!!
                    val defaultModelDataUnlocked = plugin.configManager.config.getInt("tier-icons.unlocked.custom-model-data", 0)
                    val defaultMatStrLocked = plugin.configManager.config.getString("tier-icons.locked.material", "HOPPER_MINECART")!!
                    val defaultModelDataLocked = plugin.configManager.config.getInt("tier-icons.locked.custom-model-data", 0)

                    val matStr = if (tier.customIcon == null || tier.customIcon.equals("default", true)) defaultMatStrLocked else tier.customIcon
                    val name = tier.customName ?: player.msg("gui.tier-item.name", "%tier%" to tier.level.toString())

                    // Render Free Slot
                    run {
                        val slot = freeSlots[i]
                        val matStrSlot = if (isFreeClaimed) defaultMatStrClaimed else if (isUnlocked) defaultMatStrUnlocked else matStr
                        val mat = Material.matchMaterial(matStrSlot) ?: Material.matchMaterial(defaultMatStrLocked) ?: Material.MINECART
                        val modelData = if (isFreeClaimed) defaultModelDataClaimed else if (isUnlocked) defaultModelDataUnlocked else (tier.customModelData ?: defaultModelDataLocked)

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

                        val item = ItemBuilder(mat).setName("$name &8(Free)", player).setLore(lore, player).setCustomModelData(modelData).build()
                        if (slot in 0 until size) inv.setItem(slot, item)
                    }

                    // Render Premium Slot
                    run {
                        val slot = premiumSlots[i]
                        val hasPremium = data.premium
                        val matStrSlot = if (isPremiumClaimed) defaultMatStrClaimed else if (isUnlocked && hasPremium) defaultMatStrUnlocked else matStr
                        val mat = Material.matchMaterial(matStrSlot) ?: Material.matchMaterial(defaultMatStrLocked) ?: Material.MINECART
                        val modelData = if (isPremiumClaimed) defaultModelDataClaimed else if (isUnlocked && hasPremium) defaultModelDataUnlocked else (tier.customModelData ?: defaultModelDataLocked)

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

                        val item = ItemBuilder(mat).setName("$name &e(Premium)", player).setLore(lore, player).setCustomModelData(modelData).build()
                        if (slot in 0 until size) inv.setItem(slot, item)
                    }
                }
                tierIndex++
            }
        }

        player.openInventory(inv)
        plugin.menuManager.setOpenMenu(player, MenuManager.MenuType.PASS, page)
    }
}
