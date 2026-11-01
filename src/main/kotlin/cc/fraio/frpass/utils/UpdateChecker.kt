package cc.fraio.frpass.utils

import cc.fraio.frpass.FrPass
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
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val regex = "\"tag_name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                    val match = regex.find(response)
                    
                    if (match != null) {
                        val tagName = match.groupValues[1]
                        latestVersion = tagName.replace("v", "")
                        
                        val currentVersion = plugin.description.version.replace("v", "")
                        
                        if (latestVersion != currentVersion) {
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
}
