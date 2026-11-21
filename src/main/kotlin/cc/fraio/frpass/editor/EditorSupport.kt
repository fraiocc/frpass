package cc.fraio.frpass.editor

import cc.fraio.frpass.utils.ItemBuilder
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

object EditorSupport {

    fun fillBackground(inv: Inventory, config: YamlConfiguration, size: Int) {
        val bgSection = config.getConfigurationSection("menu.items.background") ?: return
        val bgMat = bgSection.getString("material", "GRAY_STAINED_GLASS_PANE")!!
        val bgItem = ItemBuilder.fromIdentifier(bgMat).setName(" ").build()
        for (slot in bgSection.getIntegerList("slots")) {
            if (slot in 0 until size) inv.setItem(slot, bgItem)
        }
    }

    fun placeRewardListItem(
        inv: Inventory,
        config: YamlConfiguration,
        key: String,
        defaultSlot: Int,
        defaultMat: String,
        defaultName: String,
        rewards: List<String>,
        emptyText: String,
        entryFormat: String,
        player: Player,
        size: Int
    ) {
        val slot = config.getInt("menu.items.$key.slot", defaultSlot)
        if (slot !in 0 until size) return
        val mat = config.getString("menu.items.$key.material", defaultMat)!!
        val name = config.getString("menu.items.$key.name", defaultName)!!
        val lore = mutableListOf<String>()
        if (rewards.isEmpty()) {
            lore.add(emptyText)
        } else {
            rewards.forEach { lore.add(entryFormat.replace("%reward%", it)) }
        }
        val footer = config.getStringList("menu.items.$key.lore-footer").ifEmpty {
            listOf("", "&aLeft-Click: &7Add reward", "&eRight-Click: &7Remove last", "&cShift-Right-Click: &7Clear all")
        }
        lore.addAll(footer)
        inv.setItem(slot, ItemBuilder.fromIdentifier(mat).setName(name, player).setLore(lore, player).build())
    }
}
