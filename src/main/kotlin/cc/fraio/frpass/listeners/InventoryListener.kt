package cc.fraio.frpass.listeners

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.api.events.FrPassTierClaimEvent
import cc.fraio.frpass.data.PlayerData
import cc.fraio.frpass.menus.MenuManager.MenuType
import cc.fraio.frpass.utils.msg
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

class InventoryListener(private val plugin: FrPass) : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val state = plugin.menuManager.getOpenMenu(player) ?: return
        
        event.isCancelled = true
        player.updateInventory()
        
        val topInv = event.view.topInventory
        val isTopInventoryClick = event.clickedInventory == topInv || (event.rawSlot in 0 until topInv.size)
        if (!isTopInventoryClick) return

        val slot = if (event.rawSlot in 0 until topInv.size) event.rawSlot else event.slot

        when (state.first) {
            MenuType.MAIN -> handleMainMenuClick(player, slot)
            MenuType.QUESTS -> handleQuestsMenuClick(player, slot, state.second, topInv.size)
            MenuType.PASS -> handlePassMenuClick(player, slot, state.second)
        }
    }

    private fun handleMainMenuClick(player: Player, slot: Int) {
        val config = plugin.menuManager.getMenuConfig("main_menu.yml")
        val questsSlots = config.getIntegerList("menu.items.quests_button.slots")
        val passSlots = config.getIntegerList("menu.items.pass_button.slots")

        when (slot) {
            in questsSlots -> plugin.menuManager.openQuestsMenu(player, 1)
            in passSlots -> plugin.menuManager.openPassMenu(player, 1)
        }
    }

    private fun handleQuestsMenuClick(player: Player, slot: Int, page: Int, inventorySize: Int) {
        val config = plugin.menuManager.getMenuConfig("quests_menu.yml")
        val backSlots = config.getIntegerList("menu.items.back_button.slots")
        val nextSlots = config.getIntegerList("menu.items.next_page.slots")
        val prevSlots = config.getIntegerList("menu.items.prev_page.slots")
        val questSlots = config.getIntegerList("menu.quest-slots")

        val rerollEnabled = plugin.configManager.config.getBoolean("reroll.enabled", true)
        val configuredRerollSlot = plugin.configManager.config.getInt("reroll.slot", -1)
        val rerollSlot = if (configuredRerollSlot in 0 until inventorySize) configuredRerollSlot else 49

        val isRerollActive = plugin.menuManager.getRerollSelection(player) != null

        if (slot == rerollSlot && rerollEnabled) {
            handleRerollButtonClick(player, isRerollActive, page)
            return
        }

        if (slot in questSlots && isRerollActive) {
            handleQuestRerollSelection(player, slot, questSlots, page)
            return
        }

        // Navigation
        val activeQuests = plugin.playerDataManager.getPlayer(player.uniqueId)?.activeQuests?.mapNotNull { plugin.questManager.quests[it] } ?: emptyList()
        val totalPages = Math.ceil(activeQuests.size.toDouble() / questSlots.size).toInt().coerceAtLeast(1)

        when (slot) {
            in backSlots -> {
                if (isRerollActive) plugin.menuManager.stopRerollSession(player)
                plugin.menuManager.openMainMenu(player)
            }
            in nextSlots -> {
                if (page < totalPages) plugin.menuManager.openQuestsMenu(player, page + 1)
            }
            in prevSlots -> {
                if (page > 1) plugin.menuManager.openQuestsMenu(player, page - 1)
            }
        }
    }

    private fun handleRerollButtonClick(player: Player, isRerollActive: Boolean, page: Int) {
        val reqPerm = plugin.configManager.config.getBoolean("reroll.require-permission", false)
        val perm = plugin.configManager.config.getString("reroll.permission", "frpass.reroll") ?: "frpass.reroll"
        if (reqPerm && !player.hasPermission(perm)) {
            player.sendMessage(player.msg("messages.reroll-no-permission"))
            return
        }

        if (!isRerollActive) {
            plugin.menuManager.startRerollSession(player)
        } else {
            executeReroll(player)
            plugin.menuManager.stopRerollSession(player)
        }
        plugin.menuManager.openQuestsMenu(player, page)
    }

    private fun executeReroll(player: Player) {
        val selection = plugin.menuManager.getRerollSelection(player) ?: return
        if (selection.isEmpty()) return

        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
        val allAvailableQuests = plugin.questManager.quests.keys
            .filter { !data.activeQuests.contains(it) }
            .toMutableList()
            .apply { shuffle() }

        var count = 0
        for (questId in selection.toList()) {
            val idx = data.activeQuests.indexOf(questId)
            if (idx != -1 && allAvailableQuests.isNotEmpty()) {
                data.questProgress.remove(questId)
                val newQuestId = allAvailableQuests.removeAt(0)
                data.activeQuests[idx] = newQuestId
                count++
            }
        }
        plugin.playerDataManager.savePlayer(player.uniqueId)

        if (count == 0) {
            player.sendMessage(player.msg("messages.reroll-no-available-quests"))
        } else {
            player.sendMessage(player.msg("messages.reroll-success", "%count%" to count.toString()))
        }
    }

    private fun handleQuestRerollSelection(player: Player, slot: Int, questSlots: List<Int>, page: Int) {
        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
        val activeQuests = data.activeQuests.mapNotNull { plugin.questManager.quests[it] }
        val slotIndexInPage = questSlots.indexOf(slot)
        val totalIndex = ((page - 1) * questSlots.size) + slotIndexInPage

        if (totalIndex in activeQuests.indices) {
            val questId = activeQuests[totalIndex].id
            val selection = plugin.menuManager.getRerollSelection(player) ?: return
            val limit = plugin.configManager.config.getInt("reroll.limit", 3)

            if (selection.contains(questId)) {
                selection.remove(questId)
            } else {
                if (selection.size >= limit) {
                    player.sendMessage(player.msg("messages.reroll-limit-reached", "%limit%" to limit.toString()))
                    return
                }
                selection.add(questId)
            }
            plugin.menuManager.openQuestsMenu(player, page)
        }
    }

    private fun handlePassMenuClick(player: Player, slot: Int, page: Int) {
        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
        val menuConfig = plugin.menuManager.getMenuConfig("pass_menu.yml")
        
        val freeSlots = menuConfig.getIntegerList("menu.free-tier-slots")
        val premiumSlots = menuConfig.getIntegerList("menu.premium-tier-slots")
        val backSlots = menuConfig.getIntegerList("menu.items.back_button.slots")
        val nextSlots = menuConfig.getIntegerList("menu.items.next_page.slots")
        val prevSlots = menuConfig.getIntegerList("menu.items.prev_page.slots")

        when (slot) {
            in backSlots -> plugin.menuManager.openMainMenu(player)
            in nextSlots -> plugin.menuManager.openPassMenu(player, page + 1)
            in prevSlots -> if (page > 1) plugin.menuManager.openPassMenu(player, page - 1)
            in freeSlots -> claimTier(player, data, freeSlots.indexOf(slot), page, freeSlots.size, premiumSlots.size, isPremiumReward = false)
            in premiumSlots -> claimTier(player, data, premiumSlots.indexOf(slot), page, freeSlots.size, premiumSlots.size, isPremiumReward = true)
        }
    }

    private fun claimTier(
        player: Player,
        data: PlayerData,
        slotIndex: Int,
        page: Int,
        freeCount: Int,
        premiumCount: Int,
        isPremiumReward: Boolean
    ) {
        val count = Math.min(freeCount, premiumCount)
        val tierLevel = ((page - 1) * count) + slotIndex + 1
        val tier = plugin.tierManager.getTier(tierLevel) ?: return

        val alreadyClaimed = if (isPremiumReward) data.claimedPremiumTiers.contains(tierLevel) else data.claimedTiers.contains(tierLevel)
        val canClaim = data.level >= tierLevel && (!isPremiumReward || data.premium)

        if (canClaim && !alreadyClaimed) {
            if (isPremiumReward) {
                data.claimedPremiumTiers.add(tierLevel)
                plugin.rewardManager.giveRewards(player, tier.premiumRewards)
            } else {
                data.claimedTiers.add(tierLevel)
                plugin.rewardManager.giveRewards(player, tier.freeRewards)
            }

            Bukkit.getPluginManager().callEvent(FrPassTierClaimEvent(player, tierLevel, isPremiumReward))
            playClaimSound(player)
            plugin.menuManager.openPassMenu(player, page)
        }
    }

    private fun playClaimSound(player: Player) {
        val soundStr = plugin.configManager.config.getString("settings.sounds.claim-reward") ?: return
        if (soundStr.isEmpty()) return
        try {
            val sound = Sound.valueOf(soundStr.uppercase())
            player.playSound(player.location, sound, 1f, 1f)
        } catch (ignored: Exception) {}
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        if (plugin.menuManager.isPluginMenu(player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        if (!plugin.menuManager.isPluginMenu(player)) return

        plugin.foliaLib.impl.runLater(Runnable {
            if (!plugin.menuManager.isPluginMenu(player)) {
                val selection = plugin.menuManager.getRerollSelection(player)
                if (selection != null) {
                    plugin.menuManager.stopRerollSession(player)
                    if (selection.isNotEmpty()) {
                        player.sendMessage(player.msg("messages.reroll-cancelled"))
                    }
                }
            }
        }, 1L)
        plugin.menuManager.removeOpenMenu(player)
    }
}
