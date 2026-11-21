package cc.fraio.frpass.menus

import cc.fraio.frpass.utils.ColorUtils
import cc.fraio.frpass.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

object MenuSupport {

    fun createInventory(holder: InventoryHolder, size: Int, player: Player, title: String): Inventory {
        return Bukkit.createInventory(holder, size, ColorUtils.toComponent(player, title))
    }

    fun buildBackgroundItem(itemsSection: ConfigurationSection?, player: Player): ItemStack {
        val bgMat = itemsSection?.getString("background.material", "GRAY_STAINED_GLASS_PANE") ?: "GRAY_STAINED_GLASS_PANE"
        val bgModel = itemsSection?.getInt("background.custom-model-data", 0) ?: 0
        return ItemBuilder.fromIdentifier(bgMat, Material.GRAY_STAINED_GLASS_PANE)
            .setName(" ", player)
            .setCustomModelData(bgModel)
            .build()
    }

    fun placeSectionItem(
        inv: Inventory,
        section: ConfigurationSection,
        key: String,
        player: Player,
        size: Int,
        placeholder: (String) -> String = { it }
    ) {
        val matStr = section.getString("$key.material", "STONE") ?: "STONE"
        val name = placeholder(section.getString("$key.name", "") ?: "")
        val lore = section.getStringList("$key.lore").map(placeholder)
        val modelData = section.getInt("$key.custom-model-data", 0)
        val item = ItemBuilder.fromIdentifier(matStr, Material.STONE)
            .setName(name, player)
            .setLore(lore, player)
            .setCustomModelData(modelData)
            .build()
        for (slot in section.getIntegerList("$key.slots")) {
            if (slot in 0 until size) inv.setItem(slot, item)
        }
    }
}
