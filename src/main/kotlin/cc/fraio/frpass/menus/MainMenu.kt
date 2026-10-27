package cc.fraio.frpass.menus

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class MainMenu(private val plugin: FrPass) {

    fun open(player: Player) {
        val config = plugin.menuManager.getMenuConfig("main_menu.yml")
        val title = ColorUtils.colorize(player, config.getString("menu.title", "&8Main Menu")!!)
        val size = config.getInt("menu.size", 27)

        val inv = org.bukkit.Bukkit.createInventory(null, size, title)

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
                        inv.setItem(slot, item)
                    }
                }
            }
        }

        player.openInventory(inv)
        plugin.menuManager.setOpenMenu(player, MenuManager.MenuType.MAIN, 1)
    }
}
