package cc.fraio.frpass.utils

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class ItemBuilder {
    private var item: ItemStack
    private var meta: ItemMeta?

    constructor(material: Material) {
        this.item = ItemStack(material)
        this.meta = item.itemMeta
    }

    constructor(itemStack: ItemStack) {
        this.item = itemStack.clone()
        this.meta = this.item.itemMeta
    }

    fun setName(name: String, player: Player? = null): ItemBuilder {
        if (name.isNotEmpty()) {
            meta?.setDisplayName(ColorUtils.colorize(player, name))
        }
        return this
    }

    fun setLore(lore: List<String>, player: Player? = null): ItemBuilder {
        if (lore.isNotEmpty()) {
            meta?.lore = ColorUtils.colorize(player, lore)
        }
        return this
    }

    fun setCustomModelData(data: Int): ItemBuilder {
        if (data > 0) {
            meta?.setCustomModelData(data)
        }
        return this
    }

    fun setGlow(glow: Boolean): ItemBuilder {
        if (glow) {
            meta?.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true)
            meta?.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
        }
        return this
    }

    fun build(): ItemStack {
        item.itemMeta = meta
        return item
    }

    companion object {
        /**
         * Resolves an item from either Nexo ID (e.g. nexo:ruby_sword or my_nexo_item)
         * or fallback standard Bukkit Material.
         */
        fun fromIdentifier(identifier: String, defaultMaterial: Material = Material.STONE): ItemBuilder {
            val id = identifier.trim()
            
            // Try Nexo item first if Nexo plugin is loaded
            if (Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
                try {
                    val nexoId = if (id.startsWith("nexo:", ignoreCase = true)) id.substring(5) else id
                    val nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems")
                    val itemFromIdMethod = nexoItemsClass.getMethod("itemFromId", String::class.java)
                    val nexoBuilder = itemFromIdMethod.invoke(null, nexoId)
                    if (nexoBuilder != null) {
                        val buildMethod = nexoBuilder.javaClass.getMethod("build")
                        val stack = buildMethod.invoke(nexoBuilder) as? ItemStack
                        if (stack != null) {
                            return ItemBuilder(stack)
                        }
                    }
                } catch (_: Throwable) {}
            }

            val mat = Material.matchMaterial(id) ?: defaultMaterial
            return ItemBuilder(mat)
        }
    }
}
