package cc.fraio.frpass.utils

import org.bukkit.Sound
import org.bukkit.entity.Player

object SoundUtils {

    fun play(player: Player, soundName: String?, volume: Float = 1f, pitch: Float = 1f) {
        if (soundName.isNullOrBlank() || !player.isOnline) return
        try {
            val sound = Sound.valueOf(soundName.trim().uppercase())
            player.playSound(player.location, sound, volume, pitch)
        } catch (_: Exception) {}
    }
}
