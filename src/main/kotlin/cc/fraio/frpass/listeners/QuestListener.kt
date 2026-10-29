package cc.fraio.frpass.listeners

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.quests.QuestType
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerShearEntityEvent
import java.util.Collections
import java.util.UUID

class QuestListener(private val plugin: FrPass) : Listener {

    private data class BlockPos(val world: UUID, val x: Int, val y: Int, val z: Int) {
        constructor(block: Block) : this(block.world.uid, block.x, block.y, block.z)
    }

    private val placedBlocks: MutableSet<BlockPos> = Collections.synchronizedSet(
        Collections.newSetFromMap(object : LinkedHashMap<BlockPos, Boolean>(16384, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<BlockPos, Boolean>?): Boolean {
                return size > 50000
            }
        })
    )

    @EventHandler(ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (placedBlocks.remove(BlockPos(event.block))) return
        plugin.questManager.handleProgress(event.player, QuestType.BREAK_BLOCK, event.block.type.name, 1)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        placedBlocks.add(BlockPos(event.blockPlaced))
        plugin.questManager.handleProgress(event.player, QuestType.PLACE_BLOCK, event.block.type.name, 1)
    }

    @EventHandler(ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        val dir = event.direction
        val moved = event.blocks.map { BlockPos(it) }.filter { placedBlocks.remove(it) }
        for (pos in moved) {
            placedBlocks.add(BlockPos(pos.world, pos.x + dir.modX, pos.y + dir.modY, pos.z + dir.modZ))
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        val dir = event.direction
        val moved = event.blocks.map { BlockPos(it) }.filter { placedBlocks.remove(it) }
        for (pos in moved) {
            placedBlocks.add(BlockPos(pos.world, pos.x + dir.modX, pos.y + dir.modY, pos.z + dir.modZ))
        }
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        if (event.entity is Player) {
            plugin.questManager.handleProgress(killer, QuestType.KILL_PLAYER, null, 1)
        } else {
            plugin.questManager.handleProgress(killer, QuestType.KILL_MOB, event.entity.type.name, 1)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onFish(event: PlayerFishEvent) {
        if (event.state == PlayerFishEvent.State.CAUGHT_FISH) {
            plugin.questManager.handleProgress(event.player, QuestType.FISH, null, 1)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onCraft(event: CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.slotType != InventoryType.SlotType.RESULT) return

        val recipeResult = event.recipe.result
        if (recipeResult.type.isAir || recipeResult.amount <= 0) return

        val craftedAmount = when (event.click) {
            ClickType.LEFT, ClickType.RIGHT -> {
                val cursor = event.cursor
                if (!cursor.type.isAir) {
                    if (!cursor.isSimilar(recipeResult)) return
                    if (cursor.amount + recipeResult.amount > cursor.maxStackSize) return
                }
                recipeResult.amount
            }
            ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT -> {
                val minIngredient = event.inventory.matrix
                    .filterNotNull()
                    .filter { !it.type.isAir }
                    .minOfOrNull { it.amount } ?: return

                var freeSpace = 0
                for (slotItem in player.inventory.storageContents) {
                    if (slotItem == null || slotItem.type.isAir) {
                        freeSpace += recipeResult.maxStackSize
                    } else if (slotItem.isSimilar(recipeResult)) {
                        freeSpace += (recipeResult.maxStackSize - slotItem.amount).coerceAtLeast(0)
                    }
                }

                val maxBySpace = freeSpace / recipeResult.amount
                val craftCount = minOf(minIngredient, maxBySpace)
                if (craftCount <= 0) return
                craftCount * recipeResult.amount
            }
            ClickType.NUMBER_KEY -> {
                val hotbarItem = player.inventory.getItem(event.hotbarButton)
                if (hotbarItem != null && !hotbarItem.type.isAir) return
                recipeResult.amount
            }
            ClickType.SWAP_OFFHAND -> {
                if (!player.inventory.itemInOffHand.type.isAir) return
                recipeResult.amount
            }
            ClickType.DROP, ClickType.CONTROL_DROP -> {
                if (!event.cursor.type.isAir) return
                recipeResult.amount
            }
            else -> return
        }

        plugin.questManager.handleProgress(player, QuestType.CRAFT, recipeResult.type.name, craftedAmount)
    }

    @EventHandler(ignoreCancelled = true)
    fun onConsume(event: PlayerItemConsumeEvent) {
        plugin.questManager.handleProgress(event.player, QuestType.CONSUME, event.item.type.name, 1)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreed(event: EntityBreedEvent) {
        val player = event.breeder as? Player ?: return
        plugin.questManager.handleProgress(player, QuestType.BREED_ANIMAL, event.entityType.name, 1)
    }

    @EventHandler(ignoreCancelled = true)
    fun onEnchant(event: EnchantItemEvent) {
        plugin.questManager.handleProgress(event.enchanter, QuestType.ENCHANT_ITEM, event.item.type.name, 1)
    }

    @EventHandler(ignoreCancelled = true)
    fun onExpChange(event: PlayerExpChangeEvent) {
        if (event.amount > 0) {
            plugin.questManager.handleProgress(event.player, QuestType.EXP_EARN, null, event.amount)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onShear(event: PlayerShearEntityEvent) {
        plugin.questManager.handleProgress(event.player, QuestType.SHEAR_SHEEP, event.entity.type.name, 1)
    }
}
