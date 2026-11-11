package cc.fraio.frpass.hooks

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
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
        val lowerParams = params.lowercase()
        if (lowerParams.startsWith("top_")) {
            val parts = lowerParams.split("_")
            if (parts.size != 3) return null
            val rank = parts[1].toIntOrNull() ?: return null
            val entry = plugin.leaderboardManager.get(rank)
            val empty = plugin.configManager.config.getString("placeholders.top-empty", "-") ?: "-"
            return when (parts[2]) {
                "name" -> entry?.name ?: empty
                "tier" -> entry?.level?.toString() ?: empty
                "xp" -> entry?.xp?.toString() ?: empty
                else -> null
            }
        }

        if (player == null || !player.isOnline) return null
        
        val data = plugin.playerDataManager.getPlayer(player.uniqueId) ?: return "0"
        val onlinePlayer = player.player

        return when (params.lowercase()) {
            "level" -> data.level.toString()
            "tier" -> data.level.toString()
            "xp" -> data.xp.toString()
            "xp_required" -> {
                val nextTier = plugin.tierManager.getTier(data.level + 1)
                (nextTier?.requiredXp ?: 0).toString()
            }
            "xp_remaining" -> {
                val nextTier = plugin.tierManager.getTier(data.level + 1)
                if (nextTier != null) (nextTier.requiredXp - data.xp).coerceAtLeast(0).toString() else "0"
            }
            "is_premium" -> if (data.premium) "true" else "false"
            "premium_formatted" -> {
                val raw = if (data.premium) {
                    plugin.configManager.config.getString("placeholders.premium-true", "&aYes") ?: "&aYes"
                } else {
                    plugin.configManager.config.getString("placeholders.premium-false", "&cNo") ?: "&cNo"
                }
                ColorUtils.colorize(onlinePlayer, raw)
            }
            "active_quests" -> data.activeQuests.size.toString()
            "quest_reset" -> plugin.cycleManager.formatRemaining(onlinePlayer)
            else -> null
        }
    }
}
