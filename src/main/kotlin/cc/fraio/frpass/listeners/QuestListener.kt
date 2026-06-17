package cc.fraio.frpass.listeners

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.quests.QuestType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.PlayerFishEvent

class QuestListener(private val plugin: FrPass) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        plugin.questManager.handleProgress(event.player, QuestType.BREAK_BLOCK, event.block.type.name, 1)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        plugin.questManager.handleProgress(event.player, QuestType.PLACE_BLOCK, event.block.type.name, 1)
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
        val item = event.recipe.result
        plugin.questManager.handleProgress(player, QuestType.CRAFT, item.type.name, item.amount)
    }
}
