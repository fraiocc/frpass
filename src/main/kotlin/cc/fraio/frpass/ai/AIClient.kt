package cc.fraio.frpass.ai

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

class AIClient {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val lastRequestTimestamp = AtomicLong(0L)
    @Volatile var minRequestIntervalMs: Long = 3000L

    val systemPrompt: String = """
        You are Flay-0.1, an AI specialized ONLY in generating and updating Minecraft quest configuration files for the FrPass plugin.
        CRITICAL FORMAT RULES:
        1. Always return clean, valid YAML starting with 'quests:'.
        2. Supported Quest Types: BREAK_BLOCK, PLACE_BLOCK, KILL_MOB, KILL_PLAYER, FISH, WALK, PLAYTIME, CRAFT, CONSUME, BREED_ANIMAL, ENCHANT_ITEM, EXP_EARN, SHEAR_SHEEP.
        3. Reward Format: [MONEY] <amount>, [XP] <amount>, [MESSAGE] <text>, [COMMAND] <cmd>, [ITEM] <item>
        Output ONLY the YAML block, no conversational filler.
    """.trimIndent()

    fun complete(
        apiUrl: String,
        modelName: String,
        temperature: Double,
        maxTokens: Int,
        timeoutSeconds: Long,
        prompt: String
    ): CompletableFuture<String> {
        val now = System.currentTimeMillis()
        val last = lastRequestTimestamp.get()
        val elapsed = now - last

        // Cooldown check to prevent endpoint rate limit spam
        if (elapsed < minRequestIntervalMs) {
            val waitSec = ((minRequestIntervalMs - elapsed) / 1000.0)
            val future = CompletableFuture<String>()
            future.completeExceptionally(
                IllegalStateException("Rate limit active! Please wait ${String.format("%.1f", waitSec)}s.")
            )
            return future
        }
        lastRequestTimestamp.set(now)

        val jsonPayload = """
            {
                "model": "$modelName",
                "messages": [
                    {"role": "system", "content": ${escapeJson(systemPrompt)}},
                    {"role": "user", "content": ${escapeJson(prompt)}}
                ],
                "temperature": $temperature,
                "max_tokens": $maxTokens
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Content-Type", "application/json")
            .header("User-Agent", "FrPass-Minecraft-Plugin/1.0 (Java 21)")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .build()

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply { response ->
                if (response.statusCode() != 200) {
                    throw RuntimeException("HTTP ${response.statusCode()} (${response.body().take(120)})")
                }
                cleanYaml(extractContent(response.body()))
            }
    }

    // Strip markdown code fences if model enclosed response in ```yaml
    fun cleanYaml(raw: String): String {
        return when {
            raw.contains("```yaml") -> raw.substringAfter("```yaml").substringBefore("```").trim()
            raw.contains("```yml") -> raw.substringAfter("```yml").substringBefore("```").trim()
            raw.contains("```") -> raw.substringAfter("```").substringBefore("```").trim()
            else -> raw.trim()
        }
    }

    fun extractContent(json: String): String {
        val marker = "\"content\":\""
        val start = json.indexOf(marker)
        if (start == -1) return json
        val contentStart = start + marker.length

        var end = contentStart
        var escape = false
        while (end < json.length) {
            val c = json[end]
            if (c == '\\' && !escape) {
                escape = true
            } else if (c == '\"' && !escape) {
                break
            } else {
                escape = false
            }
            end++
        }

        return json.substring(contentStart, end)
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    fun escapeJson(str: String): String {
        return "\"" + str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
    }
}
