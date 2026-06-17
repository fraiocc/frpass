package cc.fraio.frpass.utils

import cc.fraio.frpass.hooks.PAPIHook
import org.bukkit.ChatColor
import org.bukkit.entity.Player

object ColorUtils {

    fun colorize(player: Player?, text: String): String {
        val papiText = PAPIHook.setPlaceholders(player, text)
        return ChatColor.translateAlternateColorCodes('&', papiText)
    }

    fun colorize(text: String): String {
        return colorize(null, text)
    }

    fun colorize(player: Player?, list: List<String>): List<String> {
        return list.map { colorize(player, it) }
    }
    
    fun colorize(list: List<String>): List<String> {
        return colorize(null, list)
    }
}
