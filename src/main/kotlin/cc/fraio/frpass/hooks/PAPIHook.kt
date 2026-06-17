package cc.fraio.frpass.hooks

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import me.clip.placeholderapi.PlaceholderAPI

object PAPIHook {
    val isAvailable: Boolean
        get() = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null

    fun setPlaceholders(player: Player?, text: String): String {
        if (!isAvailable || player == null) return text
        return PlaceholderAPI.setPlaceholders(player, text)
    }
}
