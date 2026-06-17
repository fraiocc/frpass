package cc.fraio.frpass.hooks

import cc.fraio.frpass.FrPass
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

class FrPassExpansion(private val plugin: FrPass) : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "frpass"
    }

    override fun getAuthor(): String {
        return plugin.description.authors.joinToString(", ")
    }

    override fun getVersion(): String {
        return plugin.description.version
    }

    override fun persist(): Boolean {
        return true
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null || !player.isOnline) return null
        
        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return "0"

        return when (params.lowercase()) {
            "level" -> data.level.toString()
            "tier" -> data.level.toString()
            "xp" -> data.xp.toString()
            "is_premium" -> if (data.premium) "true" else "false"
            "premium_formatted" -> if (data.premium) "&aYes" else "&cNo"
            "active_quests" -> data.activeQuests.size.toString()
            else -> null
        }
    }
}
