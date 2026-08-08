package cc.fraio.frpass.listeners

import cc.fraio.frpass.FrPass
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

class InventoryListener(private val plugin: FrPass) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val state = plugin.menuManager.getOpenMenu(player) ?: return
        
        event.isCancelled = true
        
        if (event.clickedInventory == event.view.topInventory) {
            val slot = event.slot
            
            when (state.first) {
                cc.fraio.frpass.menus.MenuManager.MenuType.MAIN -> {
                    val config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(java.io.File(plugin.dataFolder, "menus/main_menu.yml"))
                    val questsSlots = config.getIntegerList("menu.items.quests_button.slots")
                    val passSlots = config.getIntegerList("menu.items.pass_button.slots")
                    
                    if (slot in questsSlots) {
                        plugin.menuManager.openQuestsMenu(player, 1)
                    } else if (slot in passSlots) {
                        plugin.menuManager.openPassMenu(player, 1)
                    }
                }
                
                cc.fraio.frpass.menus.MenuManager.MenuType.QUESTS -> {
                    val config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(java.io.File(plugin.dataFolder, "menus/quests_menu.yml"))
                    val backSlots = config.getIntegerList("menu.items.back_button.slots")
                    val nextSlots = config.getIntegerList("menu.items.next_page.slots")
                    val prevSlots = config.getIntegerList("menu.items.prev_page.slots")
                    val questSlots = config.getIntegerList("menu.quest-slots")

                    val rerollEnabled = plugin.configManager.config.getBoolean("reroll.enabled", true)
                    var rerollSlot = plugin.configManager.config.getInt("reroll.slot", -1)
                    if (rerollSlot !in 0 until event.inventory.size) {
                        rerollSlot = 49
                    }

                    val isRerollActive = plugin.menuManager.getRerollSelection(player) != null

                    if (slot == rerollSlot && rerollEnabled) {
                        val reqPerm = plugin.configManager.config.getBoolean("reroll.require-permission", false)
                        val perm = plugin.configManager.config.getString("reroll.permission", "frpass.reroll") ?: "frpass.reroll"
                        if (reqPerm && !player.hasPermission(perm)) {
                            player.sendMessage(plugin.langManager.getMessage(player, "messages.reroll-no-permission"))
                            return
                        }

                        if (!isRerollActive) {
                            plugin.menuManager.startRerollSession(player)
                        } else {
                            val selection = plugin.menuManager.getRerollSelection(player) ?: mutableSetOf()
                            if (selection.isNotEmpty()) {
                                val data = plugin.playerDataManager.getPlayer(player.uniqueId)
                                if (data != null) {
                                    val allAvailableQuests = plugin.questManager.quests.keys.filter { !data.activeQuests.contains(it) }.toMutableList()
                                    allAvailableQuests.shuffle()

                                    var count = 0
                                    for (questId in selection.toList()) {
                                        val idx = data.activeQuests.indexOf(questId)
                                        if (idx != -1) {
                                            data.questProgress.remove(questId)
                                            if (allAvailableQuests.isNotEmpty()) {
                                                val newQuestId = allAvailableQuests.removeAt(0)
                                                data.activeQuests[idx] = newQuestId
                                            } else {
                                                data.activeQuests.removeAt(idx)
                                            }
                                            count++
                                        }
                                    }
                                    plugin.playerDataManager.savePlayer(player.uniqueId)
                                    player.sendMessage(plugin.langManager.getMessage(player, "messages.reroll-success", "%count%" to count.toString()))
                                }
                            }
                            plugin.menuManager.stopRerollSession(player)
                        }
                        plugin.menuManager.openQuestsMenu(player, state.second)
                        return
                    }

                    if (questSlots.contains(slot)) {
                        if (isRerollActive) {
                            val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
                            val activeQuests = data.activeQuests.mapNotNull { plugin.questManager.quests[it] }
                            val slotIndexInPage = questSlots.indexOf(slot)
                            val totalIndex = ((state.second - 1) * questSlots.size) + slotIndexInPage

                            if (totalIndex in activeQuests.indices) {
                                val quest = activeQuests[totalIndex]
                                val questId = quest.id
                                val selection = plugin.menuManager.getRerollSelection(player) ?: return
                                val limit = plugin.configManager.config.getInt("reroll.limit", 3)

                                if (selection.contains(questId)) {
                                    selection.remove(questId)
                                } else {
                                    if (selection.size >= limit) {
                                        player.sendMessage(plugin.langManager.getMessage(player, "messages.reroll-limit-reached", "%limit%" to limit.toString()))
                                        return
                                    }
                                    selection.add(questId)
                                }
                                plugin.menuManager.openQuestsMenu(player, state.second)
                            }
                            return
                        }
                    }

                    if (slot in backSlots) {
                        if (isRerollActive) plugin.menuManager.stopRerollSession(player)
                        plugin.menuManager.openMainMenu(player)
                    } else if (slot in nextSlots) {
                        plugin.menuManager.openQuestsMenu(player, state.second + 1)
                    } else if (slot in prevSlots) {
                        if (state.second > 1) plugin.menuManager.openQuestsMenu(player, state.second - 1)
                    }
                }
                
                cc.fraio.frpass.menus.MenuManager.MenuType.PASS -> {
                    val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return
                    val menuConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(java.io.File(plugin.dataFolder, "menus/pass_menu.yml"))
                    val freeSlots = menuConfig.getIntegerList("menu.free-tier-slots")
                    val premiumSlots = menuConfig.getIntegerList("menu.premium-tier-slots")
                    
                    val backSlots = menuConfig.getIntegerList("menu.items.back_button.slots")
                    val nextSlots = menuConfig.getIntegerList("menu.items.next_page.slots")
                    val prevSlots = menuConfig.getIntegerList("menu.items.prev_page.slots")
                    
                    if (slot in backSlots) {
                        plugin.menuManager.openMainMenu(player)
                        return
                    } else if (slot in nextSlots) {
                        plugin.menuManager.openPassMenu(player, state.second + 1)
                        return
                    } else if (slot in prevSlots) {
                        if (state.second > 1) plugin.menuManager.openPassMenu(player, state.second - 1)
                        return
                    }
                    
                    if (freeSlots.contains(slot)) {
                        val index = freeSlots.indexOf(slot)
                        val page = state.second
                        val count = Math.min(freeSlots.size, premiumSlots.size)
                        val tierLevel = ((page - 1) * count) + index + 1
                        val tier = plugin.tierManager.getTier(tierLevel) ?: return
                        
                        if (data.level >= tierLevel && !data.claimedTiers.contains(tierLevel)) {
                            data.claimedTiers.add(tierLevel)
                            plugin.rewardManager.giveRewards(player, tier.freeRewards)
                            val event = cc.fraio.frpass.api.events.FrPassTierClaimEvent(player, tierLevel, false)
                            org.bukkit.Bukkit.getPluginManager().callEvent(event)
                            val soundStr = plugin.configManager.config.getString("settings.sounds.claim-reward")
                            if (soundStr != null && soundStr.isNotEmpty()) {
                                try { player.playSound(player.location, org.bukkit.Sound.valueOf(soundStr), 1f, 1f) } catch (e: Exception) {}
                            }
                            plugin.menuManager.openPassMenu(player, page)
                        }
                    } else if (premiumSlots.contains(slot)) {
                        val index = premiumSlots.indexOf(slot)
                        val page = state.second
                        val count = Math.min(freeSlots.size, premiumSlots.size)
                        val tierLevel = ((page - 1) * count) + index + 1
                        val tier = plugin.tierManager.getTier(tierLevel) ?: return
                        
                        if (data.level >= tierLevel && data.premium && !data.claimedPremiumTiers.contains(tierLevel)) {
                            data.claimedPremiumTiers.add(tierLevel)
                            plugin.rewardManager.giveRewards(player, tier.premiumRewards)
                            val event = cc.fraio.frpass.api.events.FrPassTierClaimEvent(player, tierLevel, true)
                            org.bukkit.Bukkit.getPluginManager().callEvent(event)
                            val soundStr = plugin.configManager.config.getString("settings.sounds.claim-reward")
                            if (soundStr != null && soundStr.isNotEmpty()) {
                                try { player.playSound(player.location, org.bukkit.Sound.valueOf(soundStr), 1f, 1f) } catch (e: Exception) {}
                            }
                            plugin.menuManager.openPassMenu(player, page)
                        }
                    }
                }
            }
        }
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
        if (plugin.menuManager.isPluginMenu(player)) {
            val selection = plugin.menuManager.getRerollSelection(player)
            if (selection != null) {
                plugin.menuManager.stopRerollSession(player)
                if (selection.isNotEmpty()) {
                    player.sendMessage(plugin.langManager.getMessage(player, "messages.reroll-cancelled"))
                }
            }
            plugin.menuManager.removeOpenMenu(player)
        }
    }
}
