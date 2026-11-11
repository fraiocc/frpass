package cc.fraio.frpass.menus

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta

class LeaderboardMenu(private val plugin: FrPass) {

    fun open(player: Player) {
        val config = plugin.menuManager.getMenuConfig("leaderboard_menu.yml")
        val title = ColorUtils.colorize(player, config.getString("menu.title", "&8Leaderboard")!!)
        val size = config.getInt("menu.size", 36)

        val inv = Bukkit.createInventory(MenuInventoryHolder(MenuManager.MenuType.LEADERBOARD, 1), size, title)

        val itemsSection = config.getConfigurationSection("menu.items")
        if (itemsSection != null) {
            for (key in itemsSection.getKeys(false)) {
                if (key == "entry") continue
                val materialStr = itemsSection.getString("$key.material", "STONE")!!
                val name = itemsSection.getString("$key.name", "")!!
                val lore = itemsSection.getStringList("$key.lore")
                val modelData = itemsSection.getInt("$key.custom-model-data", 0)

                val item = ItemBuilder.fromIdentifier(materialStr, Material.STONE)
                    .setName(name, player)
                    .setLore(lore, player)
                    .setCustomModelData(modelData)
                    .build()

                for (slot in itemsSection.getIntegerList("$key.slots")) {
                    if (slot in 0 until size) inv.setItem(slot, item)
                }
            }
        }

        val entrySection = itemsSection?.getConfigurationSection("entry")
        if (entrySection != null) {
            val materialStr = entrySection.getString("material", "PLAYER_HEAD")!!
            val modelData = entrySection.getInt("custom-model-data", 0)
            val slots = config.getIntegerList("menu.entry-slots")

            for ((index, slot) in slots.withIndex()) {
                val entry = plugin.leaderboardManager.get(index + 1) ?: continue
                if (slot !in 0 until size) continue

                val replace = { text: String ->
                    text.replace("%rank%", (index + 1).toString())
                        .replace("%name%", entry.name)
                        .replace("%tier%", entry.level.toString())
                        .replace("%xp%", entry.xp.toString())
                }

                val item = ItemBuilder.fromIdentifier(materialStr, Material.PLAYER_HEAD)
                    .setName(replace(entrySection.getString("name", "")!!), player)
                    .setLore(entrySection.getStringList("lore").map(replace), player)
                    .setCustomModelData(modelData)
                    .build()

                val meta = item.itemMeta
                if (meta is SkullMeta) {
                    meta.owningPlayer = Bukkit.getOfflinePlayer(entry.uuid)
                    item.itemMeta = meta
                }
                inv.setItem(slot, item)
            }
        }

        player.openInventory(inv)
        plugin.menuManager.setOpenMenu(player, MenuManager.MenuType.LEADERBOARD, 1)
    }
}
