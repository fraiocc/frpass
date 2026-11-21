package cc.fraio.frpass.utils

import cc.fraio.frpass.hooks.PAPIHook
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import java.util.regex.Pattern

object ColorUtils {

    private val HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})")
    private val LEGACY_AMPERSAND_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])")
    private val MINI_MESSAGE_PATTERN = Pattern.compile("<(?:/?[a-zA-Z0-9_#]+|[a-zA-Z0-9_#]+:[^>]+)>")
    private val miniMessage = MiniMessage.miniMessage()
    private val legacySection = LegacyComponentSerializer.legacySection()

    private val legacyToMiniMessage = mapOf(
        '0' to "<black>",
        '1' to "<dark_blue>",
        '2' to "<dark_green>",
        '3' to "<dark_aqua>",
        '4' to "<dark_red>",
        '5' to "<dark_purple>",
        '6' to "<gold>",
        '7' to "<gray>",
        '8' to "<dark_gray>",
        '9' to "<blue>",
        'a' to "<green>",
        'b' to "<aqua>",
        'c' to "<red>",
        'd' to "<light_purple>",
        'e' to "<yellow>",
        'f' to "<white>",
        'k' to "<obfuscated>",
        'l' to "<bold>",
        'm' to "<strikethrough>",
        'n' to "<underlined>",
        'o' to "<italic>",
        'r' to "<reset>"
    )

    fun colorize(player: Player?, text: String): String {
        var result = PAPIHook.setPlaceholders(player, text)

        if (MINI_MESSAGE_PATTERN.matcher(result).find()) {
            try {
                result = legacySection.serialize(deserializeMiniMessage(result))
            } catch (_: Exception) {}
        }

        if (result.contains("&#")) {
            val matcher = HEX_PATTERN.matcher(result)
            val buffer = StringBuffer()
            while (matcher.find()) {
                val color = matcher.group(1)
                matcher.appendReplacement(buffer, "§x§${color[0]}§${color[1]}§${color[2]}§${color[3]}§${color[4]}§${color[5]}")
            }
            matcher.appendTail(buffer)
            result = buffer.toString()
        }

        if (result.contains('&')) {
            result = LEGACY_AMPERSAND_PATTERN.matcher(result).replaceAll("§$1")
        }

        return result
    }

    fun colorize(text: String): String = colorize(null, text)

    fun colorize(player: Player?, list: List<String>): List<String> = list.map { colorize(player, it) }

    fun colorize(list: List<String>): List<String> = colorize(null, list)

    fun toComponent(player: Player?, text: String): Component {
        val withPlaceholders = PAPIHook.setPlaceholders(player, text)
        if (MINI_MESSAGE_PATTERN.matcher(withPlaceholders).find()) {
            try {
                return deserializeMiniMessage(withPlaceholders)
            } catch (_: Exception) {}
        }
        return legacySection.deserialize(colorize(player, text))
    }

    fun toItemComponent(player: Player?, text: String): Component {
        return toComponent(player, text).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
    }

    fun toItemComponents(player: Player?, list: List<String>): List<Component> {
        return list.map { toItemComponent(player, it) }
    }

    private fun deserializeMiniMessage(input: String): Component {
        var converted = input.replace('§', '&')
        if (converted.contains("&#")) {
            converted = HEX_PATTERN.matcher(converted).replaceAll("<#$1>")
        }
        if (converted.contains('&')) {
            val sb = StringBuilder(converted.length)
            var i = 0
            while (i < converted.length) {
                if (converted[i] == '&' && i + 1 < converted.length) {
                    val tag = legacyToMiniMessage[converted[i + 1].lowercaseChar()]
                    if (tag != null) {
                        sb.append(tag)
                        i += 2
                        continue
                    }
                }
                sb.append(converted[i])
                i++
            }
            converted = sb.toString()
        }
        return miniMessage.deserialize(converted)
    }
}
