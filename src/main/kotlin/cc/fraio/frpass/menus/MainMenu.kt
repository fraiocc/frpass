package cc.fraio.frpass.menus

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

class MainMenu(private val plugin: FrPass) {

    fun open(player: Player) {
        val config = plugin.menuManager.getMenuConfig("main_menu.yml")
        val title = ColorUtils.colorize(player, config.getString("menu.title", "&8Main Menu")!!)
        val size = config.getInt("menu.size", 27)

        val inv = Bukkit.createInventory(MenuInventoryHolder(MenuManager.MenuType.MAIN, 1), size, title)

        val itemsSection = config.getConfigurationSection("menu.items")
        if (itemsSection != null) {
            val leaderboardEnabled = plugin.configManager.config.getBoolean("leaderboard.menu-enabled", true)
            for (key in itemsSection.getKeys(false)) {
                val isDisabledLeaderboard = key == "leaderboard_button" && !leaderboardEnabled
                val itemKey = if (isDisabledLeaderboard) "background" else key
                val materialStr = itemsSection.getString("$itemKey.material", "STONE")!!
                val name = itemsSection.getString("$itemKey.name", "")!!
                val lore = itemsSection.getStringList("$itemKey.lore")
                val modelData = itemsSection.getInt("$itemKey.custom-model-data", 0)
                val slots = itemsSection.getIntegerList("$key.slots")

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

        player.openInventory(inv)
        plugin.menuManager.setOpenMenu(player, MenuManager.MenuType.MAIN, 1)
    }
}
