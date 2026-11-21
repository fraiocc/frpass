package cc.fraio.frpass.menus

import cc.fraio.frpass.FrPass
import org.bukkit.entity.Player

class MainMenu(private val plugin: FrPass) {

    fun open(player: Player) {
        val config = plugin.menuManager.getMenuConfig("main_menu.yml")
        val size = config.getInt("menu.size", 27)
        val inv = MenuSupport.createInventory(
            MenuInventoryHolder(MenuManager.MenuType.MAIN, 1),
            size,
            player,
            config.getString("menu.title", "&8Main Menu")!!
        )

        val itemsSection = config.getConfigurationSection("menu.items")
        if (itemsSection != null) {
            val leaderboardEnabled = plugin.configManager.config.getBoolean("leaderboard.menu-enabled", true)
            val bgItem = MenuSupport.buildBackgroundItem(itemsSection, player)
            for (key in itemsSection.getKeys(false)) {
                if (key == "leaderboard_button" && !leaderboardEnabled) {
                    for (slot in itemsSection.getIntegerList("$key.slots")) {
                        if (slot in 0 until size) inv.setItem(slot, bgItem)
                    }
                    continue
                }
                MenuSupport.placeSectionItem(inv, itemsSection, key, player, size)
            }
        }

        player.openInventory(inv)
        plugin.menuManager.setOpenMenu(player, MenuManager.MenuType.MAIN, 1)
    }
}
