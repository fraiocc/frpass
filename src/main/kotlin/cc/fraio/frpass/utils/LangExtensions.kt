package cc.fraio.frpass.utils

import cc.fraio.frpass.FrPass
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

fun Player.msg(path: String, vararg placeholders: Pair<String, String>): String {
    return FrPass.instance.langManager.getMessage(this, path, *placeholders)
}

fun CommandSender.msg(path: String, vararg placeholders: Pair<String, String>): String {
    return if (this is Player) {
        FrPass.instance.langManager.getMessage(this, path, *placeholders)
    } else {
        FrPass.instance.langManager.getMessage(path, *placeholders)
    }
}

fun langMsg(path: String, vararg placeholders: Pair<String, String>): String {
    return FrPass.instance.langManager.getMessage(path, *placeholders)
}
