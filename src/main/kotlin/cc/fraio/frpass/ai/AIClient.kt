package cc.fraio.frpass.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
        3. Reward Format: [MONEY] <amount>, [XP] <amount>, [MESSAGE] <text>, [BROADCAST] <text>, [COMMAND] <cmd>, [PLAYER_COMMAND] <cmd>, [SOUND] <sound> [volume] [pitch], [TITLE] <title>|<subtitle>, [ITEM] <item>
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

        val messagesArray = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", systemPrompt)
            })
            add(JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", prompt)
            })
        }

        val requestJson = JsonObject().apply {
            addProperty("model", modelName)
            add("messages", messagesArray)
            addProperty("temperature", temperature)
            addProperty("max_tokens", maxTokens)
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Content-Type", "application/json")
            .header("User-Agent", "FrPass-Minecraft-Plugin/1.0 (Java 21)")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
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
        return try {
            val element = JsonParser.parseString(json)
            val obj = element.asJsonObject
            val choices = obj.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val firstChoice = choices[0].asJsonObject
                val message = firstChoice.getAsJsonObject("message")
                message?.get("content")?.asString ?: json
            } else {
                json
            }
        } catch (_: Exception) {
            json
        }
    }
}
