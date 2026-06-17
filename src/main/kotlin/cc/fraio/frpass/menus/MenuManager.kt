package cc.fraio.frpass.menus

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class MenuManager(private val plugin: FrPass) {
    enum class MenuType { MAIN, QUESTS, PASS }
    private val openMenus = mutableMapOf<Player, Pair<MenuType, Int>>()
    
    fun getOpenMenu(player: Player): Pair<MenuType, Int>? {
        return openMenus[player]
    }

    fun removeOpenMenu(player: Player) {
        openMenus.remove(player)
    }

    fun openMainMenu(player: Player) {
        val file = File(plugin.dataFolder, "menus/main_menu.yml")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            plugin.saveResource("menus/main_menu.yml", false)
        }
        val config = YamlConfiguration.loadConfiguration(file)
        val title = ColorUtils.colorize(player, config.getString("menu.title", "&8Main Menu")!!)
        val size = config.getInt("menu.size", 27)
        
        val inventory = Bukkit.createInventory(null as InventoryHolder?, size, title as String)
        
        val itemsSection = config.getConfigurationSection("menu.items")
        if (itemsSection != null) {
            for (key in itemsSection.getKeys(false)) {
                val materialStr = itemsSection.getString("$key.material", "STONE")!!
                val material = Material.matchMaterial(materialStr) ?: Material.STONE
                val name = itemsSection.getString("$key.name", "")!!
                val lore = itemsSection.getStringList("$key.lore")
                val modelData = itemsSection.getInt("$key.custom-model-data", 0)
                val slots = itemsSection.getIntegerList("$key.slots")
                
                val item = ItemBuilder(material)
                    .setName(name, player)
                    .setLore(lore, player)
                    .setCustomModelData(modelData)
                    .build()
                    
                for (slot in slots) {
                    if (slot in 0 until size) {
                        inventory.setItem(slot, item)
                    }
                }
            }
        }
        player.openInventory(inventory as Inventory)
        openMenus[player] = Pair(MenuType.MAIN, 1)
    }

    fun openQuestsMenu(player: Player, page: Int = 1) {
        val file = File(plugin.dataFolder, "menus/quests_menu.yml")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            plugin.saveResource("menus/quests_menu.yml", false)
        }
        val config = YamlConfiguration.loadConfiguration(file)
        val title = ColorUtils.colorize(player, config.getString("menu.title", "&8Quests Menu (Page %page%)")!!.replace("%page%", page.toString()))
        val size = config.getInt("menu.size", 54)
        
        val inventory = Bukkit.createInventory(null as InventoryHolder?, size, title as String)
        
        val questSlots = config.getIntegerList("menu.quest-slots")
        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
        val activeQuests = data.activeQuests.mapNotNull { plugin.questManager.quests[it] }
        val totalPages = Math.ceil(activeQuests.size.toDouble() / questSlots.size).toInt().coerceAtLeast(1)

        val itemsSection = config.getConfigurationSection("menu.items")
        if (itemsSection != null) {
            for (key in itemsSection.getKeys(false)) {
                val keyName = key.lowercase()
                if (keyName == "prev_page" && page <= 1) continue
                if (keyName == "next_page" && page >= totalPages) continue
                
                val materialStr = itemsSection.getString("$key.material", "STONE")!!
                val material = Material.matchMaterial(materialStr) ?: Material.STONE
                val name = itemsSection.getString("$key.name", "")!!
                val lore = itemsSection.getStringList("$key.lore")
                val modelData = itemsSection.getInt("$key.custom-model-data", 0)
                val slots = itemsSection.getIntegerList("$key.slots")
                
                val item = ItemBuilder(material)
                    .setName(name, player)
                    .setLore(lore, player)
                    .setCustomModelData(modelData)
                    .build()
                    
                for (slot in slots) {
                    if (slot in 0 until size) {
                        inventory.setItem(slot, item)
                    }
                }
            }
        }
        
        val startIndex = (page - 1) * questSlots.size
        val endIndex = Math.min(startIndex + questSlots.size, activeQuests.size)
        
        var slotIndex = 0
        for (i in startIndex until endIndex) {
            val quest = activeQuests[i]
            val progress = data.questProgress[quest.id] ?: 0
            val isCompleted = progress >= quest.requiredAmount
            
            val mat = if (isCompleted) Material.MINECART else Material.PAPER
            val qName = plugin.langManager.getMessage(player, "gui.quest-item.name", "%quest%" to (quest.displayName ?: quest.id))
            val qLore = mutableListOf(
                plugin.langManager.getMessage(player, "gui.quest-item.lore-type", "%type%" to quest.type.name),
                plugin.langManager.getMessage(player, "gui.quest-item.lore-progress", "%progress%" to progress.toString(), "%required%" to quest.requiredAmount.toString()),
                plugin.langManager.getMessage(player, "gui.quest-item.lore-reward", "%xp%" to quest.rewardXp.toString())
            )
            if (isCompleted) qLore.add(plugin.langManager.getMessage(player, "gui.quest-item.lore-completed"))
            
            val item = ItemBuilder(mat).setName(qName, player).setLore(qLore, player).build()
            inventory.setItem(questSlots[slotIndex], item)
            slotIndex++
        }
        
        player.openInventory(inventory)
        openMenus[player] = Pair(MenuType.QUESTS, page)
    }

    fun openPassMenu(player: Player, page: Int = 1) {
        val menuConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(java.io.File(plugin.dataFolder, "menus/pass_menu.yml"))
        val titleRaw = menuConfig.getString("menu.title", "&8Battlepass (Page %page%)")!!.replace("%page%", page.toString())
        val title = ColorUtils.colorize(player, titleRaw)
        val size = menuConfig.getInt("menu.size", 27)
        
        val inventory = Bukkit.createInventory(null as InventoryHolder?, size, title as String)
        
        // Load custom items
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
                    inventory.setItem(slot, item)
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
                    val name = tier.customName ?: plugin.langManager.getMessage(player, "gui.tier-item.name", "%tier%" to tier.level.toString())
                    
                    // Render Free Slot
                    run {
                        val slot = freeSlots[i]
                        val matStrSlot = if (isFreeClaimed) defaultMatStrClaimed else if (isUnlocked) defaultMatStrUnlocked else matStr
                        val mat = Material.matchMaterial(matStrSlot) ?: Material.matchMaterial(defaultMatStrLocked) ?: Material.MINECART
                        val modelData = if (isFreeClaimed) defaultModelDataClaimed else if (isUnlocked) defaultModelDataUnlocked else (tier.customModelData ?: defaultModelDataLocked)
                        
                        val statusText = if (isFreeClaimed) plugin.langManager.getMessage(player, "gui.tier-item.status-claimed")
                        else if (isUnlocked) plugin.langManager.getMessage(player, "gui.tier-item.status-unlocked")
                        else plugin.langManager.getMessage(player, "gui.tier-item.status-locked")
                        
                        val lore = mutableListOf<String>()
                        if (tier.customLore != null) {
                            lore.addAll(tier.customLore)
                        } else {
                            lore.add(plugin.langManager.getMessage(player, "gui.tier-item.lore-xp", "%xp%" to tier.requiredXp.toString()))
                        }
                        lore.add("")
                        lore.add(plugin.langManager.getMessage(player, "gui.tier-item.status-prefix") + statusText)
                        
                        val item = ItemBuilder(mat).setName("$name &8(Free)", player).setLore(lore, player).setCustomModelData(modelData).build()
                        if (slot in 0 until size) inventory.setItem(slot, item)
                    }
                    
                    // Render Premium Slot
                    run {
                        val slot = premiumSlots[i]
                        val hasPremium = data.premium
                        val matStrSlot = if (isPremiumClaimed) defaultMatStrClaimed else if (isUnlocked && hasPremium) defaultMatStrUnlocked else matStr
                        val mat = Material.matchMaterial(matStrSlot) ?: Material.matchMaterial(defaultMatStrLocked) ?: Material.MINECART
                        val modelData = if (isPremiumClaimed) defaultModelDataClaimed else if (isUnlocked && hasPremium) defaultModelDataUnlocked else (tier.customModelData ?: defaultModelDataLocked)
                        
                        val statusText = if (isPremiumClaimed) plugin.langManager.getMessage(player, "gui.tier-item.status-claimed")
                        else if (isUnlocked && hasPremium) plugin.langManager.getMessage(player, "gui.tier-item.status-unlocked")
                        else plugin.langManager.getMessage(player, "gui.tier-item.status-locked")
                        
                        val lore = mutableListOf<String>()
                        if (tier.customLore != null) {
                            lore.addAll(tier.customLore)
                        } else {
                            lore.add(plugin.langManager.getMessage(player, "gui.tier-item.lore-xp", "%xp%" to tier.requiredXp.toString()))
                        }
                        if (!hasPremium) {
                            lore.add("")
                            lore.add(ColorUtils.colorize(player, "&cRequires Premium!"))
                        }
                        lore.add("")
                        lore.add(plugin.langManager.getMessage(player, "gui.tier-item.status-prefix") + statusText)
                        
                        val item = ItemBuilder(mat).setName("$name &e(Premium)", player).setLore(lore, player).setCustomModelData(modelData).build()
                        if (slot in 0 until size) inventory.setItem(slot, item)
                    }
                }
                tierIndex++
            }
        }
        
        player.openInventory(inventory as Inventory)
        openMenus[player] = Pair(MenuType.PASS, page)
    }

    fun isPluginMenu(player: Player): Boolean {
        return openMenus.containsKey(player)
    }
}
