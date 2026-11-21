package cc.fraio.frpass.utils

import cc.fraio.frpass.FrPass
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.function.Consumer

class UpdateChecker(private val plugin: FrPass) {

    private val repo = "fraiocc/frpass"
    var updateAvailable = false
    var latestVersion = ""
    var downloadLink = "https://github.com/$repo/releases/latest"

    fun checkForUpdates() {
        plugin.foliaLib.scheduler.runAsync(Consumer { _ ->
            try {
                val url = URI.create("https://api.github.com/repos/$repo/releases/latest").toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "FrPass-UpdateChecker")
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val regex = "\"tag_name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                    val match = regex.find(response)

                    if (match != null) {
                        latestVersion = match.groupValues[1].removePrefix("v")
                        val currentVersion = plugin.pluginMeta.version.removePrefix("v")

                        if (isNewerVersion(latestVersion, currentVersion)) {
                            updateAvailable = true
                            plugin.logger.warning("==========================================")
                            plugin.logger.warning("New Update!! Download: $downloadLink")
                            plugin.logger.warning("Current version: $currentVersion | Latest: $latestVersion")
                            plugin.logger.warning("==========================================")
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("Could not check for updates: ${e.message}")
            }
        })
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = parseParts(latest)
        val currentParts = parseParts(current)
        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l != c) return l > c
        }
        return false
    }

    private fun parseParts(version: String): List<Int> {
        return version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    }
}
