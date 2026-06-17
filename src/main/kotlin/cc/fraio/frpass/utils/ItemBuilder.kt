package cc.fraio.frpass.utils

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

import org.bukkit.entity.Player

class ItemBuilder(material: Material) {
    private val item: ItemStack = ItemStack(material)
    private val meta: ItemMeta? = item.itemMeta

    fun setName(name: String, player: Player? = null): ItemBuilder {
        meta?.setDisplayName(ColorUtils.colorize(player, name))
        return this
    }

    fun setLore(lore: List<String>, player: Player? = null): ItemBuilder {
        meta?.lore = ColorUtils.colorize(player, lore)
        return this
    }

    fun setCustomModelData(data: Int): ItemBuilder {
        if (data > 0) {
            meta?.setCustomModelData(data)
        }
        return this
    }

    fun build(): ItemStack {
        item.itemMeta = meta
        return item
    }
}
