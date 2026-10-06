package cc.fraio.frpass.utils

import cc.fraio.frpass.hooks.PAPIHook
import org.bukkit.ChatColor
import org.bukkit.entity.Player

object ColorUtils {

    private val HEX_PATTERN = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})")

    fun colorize(player: Player?, text: String): String {
        var result = PAPIHook.setPlaceholders(player, text)
        
        // Hex color support: &#RRGGBB
        val matcher = HEX_PATTERN.matcher(result)
        val buffer = StringBuffer()
        while (matcher.find()) {
            val color = matcher.group(1)
            matcher.appendReplacement(buffer, "§x§${color[0]}§${color[1]}§${color[2]}§${color[3]}§${color[4]}§${color[5]}")
        }
        matcher.appendTail(buffer)
        result = buffer.toString()

        return ChatColor.translateAlternateColorCodes('&', result)
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
